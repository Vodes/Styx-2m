package moe.styx.styx2m.player.mpvkit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.russhwolf.settings.get
import moe.styx.common.compose.settings
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

class MpvKitIosBackend(
    private val sink: PlayerBackendSink,
    private val bridge: MpvKitPlayerBridge
) : PlayerBackendWithSurface {
    override val id = PlayerBackendId.MPV
    override val capabilities = PlayerBackendCapabilities(
        supportsCacheEnd = true,
        supportsChapters = true,
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
        if (canChangeTrack()) bridge.setAudioTrack(id)
    }

    override fun setSubtitleTrack(id: Int) {
        if (canChangeTrack()) bridge.setSubtitleTrack(id)
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
            sink.onError("MPV", "Failed to release mpv.", it)
        }
        libWasLoaded = false
        playerInitialized = false
        playbackStatus = PlaybackStatus.Idle
        pendingLoad = null
    }

    @Composable
    override fun RenderSurface(modifier: Modifier, preferences: MediaPreferences?) {
        val options = remember(preferences) { MpvKitOptions.from(preferences) }
        val renderView = remember(options.gpuApi) { bridge.getPlayerView(options.gpuApi) }
        ensureInitialized(options)

        UIKitView(
            factory = { renderView },
            modifier = modifier.fillMaxSize().onSizeChanged {
                Log.d("MPVKit") { "Compose render size: ${it.width}x${it.height}" }
                bridge.resizeSurface(it.width, it.height)
            },
            update = {
                it.setNeedsLayout()
                it.layoutIfNeeded()
            },
            properties = UIKitInteropProperties(isInteractive = false, isNativeAccessibilityEnabled = false)
        )
    }

    private fun ensureInitialized(options: MpvKitOptions) {
        if (libWasLoaded) return

        bridge.onPropertyChange = letProperty@{ name, value ->
            when (name) {
                "time-pos" -> (value as? Double)?.let { sink.onProgress(it.toLong()) }
                "duration" -> (value as? Double)?.let { sink.onDuration(it.toLong()) }
                "demuxer-cache-time" -> (value as? Double)?.let { sink.onCacheEnd(it.toLong()) }
                "percent-pos" -> (value as? Double)?.let { sink.onPlaybackPercent(it.toFloat()) }
                "pause" -> {
                    val paused = value as? Boolean ?: return@letProperty
                    isPaused = paused
                    if (isNotBuffering()) updateStatus(if (paused) PlaybackStatus.Paused else PlaybackStatus.Playing)
                }
                "paused-for-cache" -> {
                    val buffering = value as? Boolean ?: return@letProperty
                    if (playerInitialized) updateStatus(if (buffering) PlaybackStatus.Buffering else if (isPaused) PlaybackStatus.Paused else PlaybackStatus.Playing)
                }
                "seeking" -> {
                    val seeking = value as? Boolean ?: return@letProperty
                    if (isNotBuffering()) {
                        updateStatus(
                            if (seeking) PlaybackStatus.Seeking
                            else if (isPaused) PlaybackStatus.Paused
                            else PlaybackStatus.Playing
                        )
                    }
                }
                "metadata/by-key/title" -> (value as? String)?.trim()?.takeIf(String::isNotBlank)?.let(sink::onMediaTitle)
                else -> Log.d("MPVKit") { "Property changed - $name: $value" }
            }
        }

        bridge.onEvent = { eventName ->
            when (eventName) {
                "file-loaded" -> {
                    playerInitialized = true
                    updateTracks()
                    updateChapters()
                    updateStatus(if (isPaused) PlaybackStatus.Paused else PlaybackStatus.Playing)
                }
                "end-file" -> updateStatus(PlaybackStatus.EOF)
                else -> Log.d("MPVKit") { "Event - $eventName" }
            }
        }

        bridge.create(
            videoOutputDriver = options.videoOutputDriver,
            gpuApi = options.gpuApi,
            profile = options.profile,
            hwdec = options.hwdec,
            deband = options.deband,
            debandIterations = options.debandIterations,
            dither10bit = options.dither10bit,
            blendSubtitles = options.blendSubtitles,
            slang = options.slang,
            alang = options.alang,
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

    private fun updateChapters() {
        val chapters = (0 until bridge.getChapterCount()).map { index ->
            Chapter(
                title = bridge.getChapterTitle(index) ?: "Chapter ${index + 1}",
                time = bridge.getChapterTime(index).toFloat()
            )
        }
        sink.onChapters(chapters)
    }

    private fun updateStatus(status: PlaybackStatus) {
        playbackStatus = status
        sink.onPlaybackStatus(status)
    }

    private fun isNotBuffering(): Boolean = playbackStatus !is PlaybackStatus.Buffering

    private fun canChangeTrack(): Boolean {
        return playerInitialized && playbackStatus in arrayOf(PlaybackStatus.Playing, PlaybackStatus.Paused)
    }
}

private data class MpvKitOptions(
    val videoOutputDriver: String,
    val gpuApi: String,
    val profile: String,
    val hwdec: String,
    val deband: Boolean,
    val debandIterations: String,
    val dither10bit: Boolean,
    val blendSubtitles: Boolean,
    val slang: String,
    val alang: String,
) {
    companion object {
        fun from(mediaPreferences: MediaPreferences?): MpvKitOptions {
            val pref = MpvPreferences.getOrDefault()
            return MpvKitOptions(
                videoOutputDriver = pref.videoOutputDriver,
                gpuApi = pref.gpuAPI,
                profile = pref.getPlatformProfile(),
                hwdec = when (settings["hwdec", "copy"]) {
                    "yes" -> "videotoolbox"
                    "copy" -> "videotoolbox-copy"
                    else -> "no"
                },
                deband = pref.deband,
                debandIterations = pref.debandIterations,
                dither10bit = pref.dither10bit,
                blendSubtitles = pref.blendSubs,
                slang = pref.getSlangArg(mediaPreferences),
                alang = pref.getAlangArg(mediaPreferences),
            )
        }
    }
}
