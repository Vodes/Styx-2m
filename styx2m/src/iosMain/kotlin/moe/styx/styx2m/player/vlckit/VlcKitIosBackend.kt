package moe.styx.styx2m.player.vlckit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import moe.styx.common.compose.utils.MpvPreferences
import moe.styx.common.data.MediaPreferences
import moe.styx.common.util.Log
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.player.PlaybackSource
import moe.styx.styx2m.player.PlaybackStatus
import moe.styx.styx2m.player.PlayerBackendCapabilities
import moe.styx.styx2m.player.PlayerBackendId
import moe.styx.styx2m.player.PlayerBackendSink
import moe.styx.styx2m.player.PlayerBackendWithSurface
import moe.styx.styx2m.player.PlayerTrack
import moe.styx.styx2m.player.PlayerTrackType

class VlcKitIosBackend(
    private val sink: PlayerBackendSink,
    private val bridge: VlcKitPlayerBridge
) : PlayerBackendWithSurface {
    override val id = PlayerBackendId.VLC
    override val capabilities = PlayerBackendCapabilities(
        supportsCacheEnd = false,
        supportsChapters = false,
        supportsAudioTrackSelection = true,
        supportsSubtitleTrackSelection = true,
        supportsLocalFiles = true,
        supportsHttpStreams = true
    )

    private var libWasLoaded = false
    private var playerInitialized = false
    private var isPaused = false
    private var playbackStatus: PlaybackStatus = PlaybackStatus.Idle
    private var pendingLoad: Pair<PlaybackSource, Long>? = null
    private var pendingAudioTrackId: Int? = null
    private var pendingSubtitleTrackId: Int? = null
    private var lastProgressSeconds = 0L
    private var lastDurationSeconds = 0L

    override fun load(source: PlaybackSource, startAt: Long) {
        if (!libWasLoaded) {
            pendingLoad = source to startAt
            return
        }

        when (source) {
            is PlaybackSource.LocalFile -> bridge.loadFile(source.path, startAt.toDouble())
            is PlaybackSource.RemoteUrl -> bridge.loadUrl(source.url, startAt.toDouble())
            is PlaybackSource.Unavailable -> showMessage(source.message)
        }
    }

    override fun setPlaying(playing: Boolean) {
        if (libWasLoaded) bridge.setPaused(!playing)
    }

    override fun seek(position: Long) {
        if (playerInitialized) bridge.seekTo(position.toDouble())
    }

    override fun setAudioTrack(id: Int) {
        if (canChangeTrack()) {
            bridge.setAudioTrack(id)
        } else {
            pendingAudioTrackId = id
        }
    }

    override fun setSubtitleTrack(id: Int) {
        if (canChangeTrack()) {
            bridge.setSubtitleTrack(id)
        } else {
            pendingSubtitleTrackId = id
        }
    }

    override fun showMessage(message: String, durationMillis: Int) {
        if (libWasLoaded) bridge.showMessage(message, durationMillis)
    }

    override fun release() {
        runCatching {
            bridge.onPropertyChange = null
            bridge.onEvent = null
            if (libWasLoaded) bridge.destroy()
        }.onFailure {
            sink.onError("VLC", "Failed to release vlc.", it)
        }
        libWasLoaded = false
        playerInitialized = false
        playbackStatus = PlaybackStatus.Idle
        pendingLoad = null
        pendingAudioTrackId = null
        pendingSubtitleTrackId = null
        lastProgressSeconds = 0L
        lastDurationSeconds = 0L
    }

    @Composable
    override fun RenderSurface(modifier: Modifier, preferences: MediaPreferences?) {
        val options = remember(preferences) { VlcKitOptions.from(preferences) }
        val renderView = remember { bridge.getPlayerView() }
        ensureInitialized(options)

        UIKitView(
            factory = { renderView },
            modifier = modifier.fillMaxSize(),
            properties = UIKitInteropProperties(isInteractive = false, isNativeAccessibilityEnabled = false)
        )
    }

    private fun ensureInitialized(options: VlcKitOptions) {
        if (libWasLoaded) return

        bridge.onPropertyChange = letProperty@{ name, value ->
            when (name) {
                "time-pos" -> (value as? Double)?.let {
                    lastProgressSeconds = it.toLong()
                    sink.onProgress(lastProgressSeconds)
                    syncPlaybackPercent()
                    if (playerInitialized && !isPaused && playbackStatus == PlaybackStatus.Buffering) {
                        updateStatus(PlaybackStatus.Playing)
                    }
                }
                "duration" -> (value as? Double)?.let {
                    lastDurationSeconds = it.toLong()
                    sink.onDuration(lastDurationSeconds)
                    syncPlaybackPercent()
                }
                "pause" -> {
                    val paused = value as? Boolean ?: return@letProperty
                    isPaused = paused
                    updateStatus(if (paused) PlaybackStatus.Paused else PlaybackStatus.Playing)
                }
                else -> Log.d("VLC") { "Property changed - $name: $value" }
            }
        }

        bridge.onEvent = { eventName ->
            when (eventName) {
                "playing" -> {
                    playerInitialized = true
                    updateStatus(PlaybackStatus.Playing)
                    applyPendingTrackSelection()
                    updateTracks()
                }
                "paused" -> updateStatus(PlaybackStatus.Paused)
                "buffering" -> updateStatus(PlaybackStatus.Buffering)
                "end-file" -> updateStatus(PlaybackStatus.EOF)
                "stopped" -> updateStatus(PlaybackStatus.Idle)
                "error" -> sink.onError("VLC", "VLC playback failed.")
                "tracks" -> updateTracks()
                else -> Log.d("VLC") { "Event - $eventName" }
            }
        }

        bridge.create(
            slang = options.slang,
            alang = options.alang,
            hardwareDecoding = options.hardwareDecoding,
        )
        libWasLoaded = true
        pendingLoad?.let { (source, startAt) ->
            pendingLoad = null
            load(source, startAt)
        }
    }

    private fun updateTracks() {
        val tracks = (0 until bridge.getTrackCount()).mapNotNull { index ->
            val type = when (bridge.getTrackType(index)?.lowercase()) {
                "audio" -> PlayerTrackType.AUDIO
                "sub" -> PlayerTrackType.SUBTITLE
                "video" -> PlayerTrackType.VIDEO
                else -> PlayerTrackType.UNKNOWN
            }
            if (type == PlayerTrackType.UNKNOWN) return@mapNotNull null

            PlayerTrack(
                id = bridge.getTrackId(index),
                type = type,
                title = bridge.getTrackTitle(index),
                language = bridge.getTrackLang(index),
                isDefault = bridge.isTrackDefault(index),
                isForced = bridge.isTrackForced(index),
                isSelected = bridge.isTrackSelected(index),
                codec = bridge.getTrackCodec(index)
            )
        }
        sink.onTracks(tracks)
    }

    private fun updateStatus(status: PlaybackStatus) {
        if (playbackStatus == status) return
        playbackStatus = status
        sink.onPlaybackStatus(status)
    }

    private fun syncPlaybackPercent() {
        sink.onPlaybackPercent(
            if (lastDurationSeconds > 0L) {
                lastProgressSeconds.toFloat() / lastDurationSeconds.toFloat() * 100F
            } else {
                0F
            }
        )
    }

    private fun canChangeTrack(): Boolean {
        return playerInitialized && playbackStatus in arrayOf(PlaybackStatus.Playing, PlaybackStatus.Paused)
    }

    private fun applyPendingTrackSelection() {
        pendingAudioTrackId?.let { id ->
            pendingAudioTrackId = null
            bridge.setAudioTrack(id)
        }
        pendingSubtitleTrackId?.let { id ->
            pendingSubtitleTrackId = null
            bridge.setSubtitleTrack(id)
        }
    }
}

private data class VlcKitOptions(
    val slang: String,
    val alang: String,
    val hardwareDecoding: Boolean,
) {
    companion object {
        fun from(mediaPreferences: MediaPreferences?): VlcKitOptions {
            val pref = MpvPreferences.getOrDefault()
            return VlcKitOptions(
                slang = "",
                alang = "",
                hardwareDecoding = pref.hwDecoding,
            )
        }
    }
}
