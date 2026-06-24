package moe.styx.styx2m.player.mpv

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.russhwolf.settings.get
import dev.jdtech.mpv.MPVLib
import moe.styx.common.compose.AppContextImpl.appConfig
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.MpvPreferences
import moe.styx.common.data.MediaPreferences
import moe.styx.common.extension.eqI
import moe.styx.common.json
import moe.styx.common.util.Log
import moe.styx.common.util.SYSTEMFILES
import moe.styx.styx2m.misc.Track
import moe.styx.styx2m.misc.toPlayerTrack
import moe.styx.styx2m.player.PlaybackSource
import moe.styx.styx2m.player.PlaybackStatus
import moe.styx.styx2m.player.PlayerBackendCapabilities
import moe.styx.styx2m.player.PlayerBackendId
import moe.styx.styx2m.player.PlayerBackendSink
import moe.styx.styx2m.player.PlayerBackendWithSurface
import okio.Path.Companion.toPath
import java.io.FileOutputStream

class MpvAndroidBackend(
    private val sink: PlayerBackendSink
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

    private var playerInitialized = false
    private var libWasLoaded = false
    private var isPaused = false
    private var player: MPVLib? = null
    private var playbackStatus: PlaybackStatus = PlaybackStatus.Idle
    private var pendingLoad: Pair<PlaybackSource, Long>? = null

    private val observer by lazy {
        object : MPVLib.EventObserver {
            override fun event(eventId: Int) {
                when (eventId) {
                    MPVLib.MpvEvent.MPV_EVENT_SEEK -> {
                        updateStatus(PlaybackStatus.Seeking)
                    }

                    MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                        if (!playerInitialized) {
                            playerInitialized = true
                            player?.setPropertyBoolean("pause", false)
                        }
                    }

                    else -> Unit
                }
            }

            override fun eventProperty(property: String) {}

            override fun eventProperty(property: String, value: Long) {
                when (property) {
                    "time-pos" -> sink.onProgress(value)
                    "duration" -> sink.onDuration(value)
                    "demuxer-cache-time" -> sink.onCacheEnd(value)
                    else -> Log.d("MPV") { "Long property changed - $property: $value" }
                }
            }

            override fun eventProperty(property: String, value: Double) {
                when (property) {
                    "percent-pos" -> sink.onPlaybackPercent(value.toFloat())
                    else -> Log.d("MPV") { "Double property changed - $property: $value" }
                }
            }

            override fun eventProperty(property: String, value: Boolean) {
                when (property) {
                    "pause" -> {
                        isPaused = value
                        if (isNotBuffering()) {
                            updateStatus(if (value) PlaybackStatus.Paused else PlaybackStatus.Playing)
                        }
                    }

                    "paused-for-cache" -> {
                        if (playerInitialized) {
                            updateStatus(if (value) PlaybackStatus.Buffering else PlaybackStatus.Playing)
                        }
                    }

                    "seeking" -> {
                        if (isNotBuffering()) {
                            updateStatus(
                                if (value) PlaybackStatus.Seeking
                                else if (isPaused) PlaybackStatus.Paused
                                else PlaybackStatus.Playing
                            )
                        }
                    }

                    else -> Log.d("MPV") { "Bool property changed - $property: $value" }
                }
            }

            override fun eventProperty(property: String, value: String) {
                when (property) {
                    "metadata/by-key/title" -> {
                        val title = value.trim()
                        if (title.isNotBlank()) {
                            sink.onMediaTitle(title)
                        }
                    }
                    "track-list" -> {
                        runCatching {
                            sink.onTracks(json.decodeFromString<List<Track>>(value).map(Track::toPlayerTrack))
                        }.onFailure {
                            sink.onError("MPV", "Failed to parse track-list!", it)
                        }
                    }

                    "chapter-list" -> {
                        runCatching {
                            sink.onChapters(json.decodeFromString(value))
                        }.onFailure {
                            sink.onError("MPV", "Failed to parse chapter-list!", it)
                        }
                    }

                    else -> Log.d("MPV") { "String property changed - $property: $value" }
                }
            }
        }
    }

    override fun load(source: PlaybackSource, startAt: Long) {
        if (!libWasLoaded) {
            pendingLoad = source to startAt
            return
        }

        when (source) {
            is PlaybackSource.LocalFile -> loadFile(source.path, startAt)
            is PlaybackSource.RemoteUrl -> loadFile(source.url, startAt)
            is PlaybackSource.Unavailable -> showMessage(source.message)
        }
    }

    override fun setPlaying(playing: Boolean) {
        if (playerInitialized) {
            player?.setPropertyBoolean("pause", !playing)
        }
    }

    override fun seek(position: Long) {
        if (playerInitialized) {
            player?.command(arrayOf("set", "time-pos", "$position"))
        }
    }

    override fun setSubtitleTrack(id: Int) {
        if (canChangeTrack()) {
            player?.command(arrayOf("set", "sub", if (id == -1) "no" else "$id"))
        }
    }

    override fun setAudioTrack(id: Int) {
        if (canChangeTrack()) {
            player?.command(arrayOf("set", "audio", "$id"))
        }
    }

    override fun showMessage(message: String, durationMillis: Int) {
        if (libWasLoaded) {
            player?.command(arrayOf("show-text", message, "$durationMillis"))
        }
    }

    override fun release() {
        runCatching {
            if (libWasLoaded) {
                player?.removeObserver(observer)
                player?.destroy()
            }
        }.onFailure {
            sink.onError("MPV", "Failed to release mpv.", it)
        }
        playerInitialized = false
        libWasLoaded = false
        playbackStatus = PlaybackStatus.Idle
        pendingLoad = null
    }

    @Composable
    override fun RenderSurface(modifier: Modifier, preferences: MediaPreferences?) {
        val context = LocalContext.current
        val holderCallback = remember { surfaceHolderCallback() }
        ensureInitialized(context, preferences)

        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(holderCallback)
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }

    private fun ensureInitialized(context: Context, preferences: MediaPreferences?) {
        if (libWasLoaded) return

        player = MPVLib.create(context)
        setMPVOptions(context, preferences)
        player!!.init()
        initObservers()
        libWasLoaded = true
        pendingLoad?.let { (source, startAt) ->
            pendingLoad = null
            load(source, startAt)
        }
    }

    private fun loadFile(pathOrUrl: String, startAt: Long) {
        player?.command(arrayOf("set", "start", "$startAt"))
        player?.command(arrayOf("loadfile", pathOrUrl, "replace"))
    }

    private fun initObservers() {
        player?.addObserver(observer)

        arrayOf(
            Property("track-list", MPVLib.MpvFormat.MPV_FORMAT_STRING),
            Property("chapter-list", MPVLib.MpvFormat.MPV_FORMAT_STRING),
            Property("paused-for-cache", MPVLib.MpvFormat.MPV_FORMAT_FLAG),
            Property("eof-reached", MPVLib.MpvFormat.MPV_FORMAT_FLAG),
            Property("seekable", MPVLib.MpvFormat.MPV_FORMAT_FLAG),
            Property("time-pos", MPVLib.MpvFormat.MPV_FORMAT_INT64),
            Property("percent-pos", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE),
            Property("duration", MPVLib.MpvFormat.MPV_FORMAT_INT64),
            Property("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG),
            Property("seeking", MPVLib.MpvFormat.MPV_FORMAT_FLAG),
            Property("demuxer-cache-time", MPVLib.MpvFormat.MPV_FORMAT_INT64),
            Property("metadata/by-key/title", MPVLib.MpvFormat.MPV_FORMAT_STRING)
        ).forEach { (name, format) ->
            player?.observeProperty(name, format)
        }
    }

    private fun setMPVOptions(context: Context, preferences: MediaPreferences?) {
        val configDir = appConfig().appStoragePath.toPath() / "mpv"
        SYSTEMFILES.createDirectory(configDir)
        if ((SYSTEMFILES.listOrNull(configDir) ?: emptyList()).find { it.name eqI "subfont.ttf" } == null) {
            runCatching {
                val fontResource = context.resources.getIdentifier("subfont", "raw", context.packageName)
                require(fontResource != 0) { "Missing raw resource: subfont" }
                val fontStream = context.resources.openRawResource(fontResource)
                val outputFile = configDir / "subfont.ttf"
                fontStream.copyTo(FileOutputStream(outputFile.toFile()))
            }.onFailure {
                Log.e("MPV", exception = it) { "Failed to copy font to mpv config dir." }
            }.onSuccess {
                Log.i("MPV") { "Copied subfont.ttf file to mpv config dir." }
            }
        }

        val pref = MpvPreferences.getOrDefault()
        player?.setOptionString("config", "yes")
        player?.setOptionString("config-dir", configDir.toString())
        player?.setOptionString("profile", pref.getPlatformProfile())
        if (pref.profile == "normal") {
            player?.setOptionString("scale", "catmull_rom")
            player?.setOptionString("cscale", "bilinear")
            player?.setOptionString("dscale", "bilinear")
        }

        player?.setOptionString("dither", if (pref.profile == "high") "fruit" else "ordered")
        player?.setOptionString("gpu-api", pref.gpuAPI)
        player?.setOptionString("gpu-context", "android")
        player?.setOptionString("opengl-es", "yes")
        player?.setOptionString("tls-verify", "no")
        player?.setOptionString("blend-subtitles", "yes")
        player?.setOptionString("cache", "yes")
        player?.setOptionString("demuxer-max-bytes", "50MiB")
        player?.setOptionString("demuxer-max-back-bytes", "32MiB")
        player?.setOptionString("force-window", "no")
        player?.setOptionString("keep-open", "always")
        player?.setOptionString("ytdl", "no")
        player?.setOptionString("save-position-on-quit", "no")
        player?.setOptionString("sub-font-provider", "none")
        player?.setOptionString(
            "hwdec", when (settings["hwdec", "copy"]) {
                "yes" -> "mediacodec"
                "copy" -> "mediacodec-copy"
                else -> "no"
            }
        )
        player?.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        player?.setOptionString("deband", if (pref.deband) "yes" else "no")
        player?.setOptionString("deband-iterations", pref.debandIterations)
        player?.setOptionString("dither-depth", if (pref.dither10bit) "10" else "8")
        player?.setOptionString("slang", pref.getSlangArg(preferences))
        player?.setOptionString("alang", pref.getAlangArg(preferences))
    }

    private fun surfaceHolderCallback(): SurfaceHolder.Callback {
        return object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                player?.attachSurface(holder.surface)
                player?.setOptionString("force-window", "yes")
                player?.setOptionString("vo", MpvPreferences.getOrDefault().videoOutputDriver)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
                player?.setPropertyString("android-surface-size", "${width}x$height")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                player?.setOptionString("vo", "null")
                player?.setOptionString("force-window", "no")
                player?.detachSurface()
            }
        }
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

private data class Property(val name: String, val format: Int)
