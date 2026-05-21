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
import moe.styx.styx2m.player.PlayerBackend
import moe.styx.styx2m.player.PlayerBackendCapabilities
import moe.styx.styx2m.player.PlayerBackendId
import moe.styx.styx2m.player.PlayerBackendSink
import okio.Path.Companion.toPath
import java.io.FileOutputStream

class MpvAndroidBackend(
    private val sink: PlayerBackendSink
) : PlayerBackend {
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
    private var playbackStatus: PlaybackStatus = PlaybackStatus.Idle
    private var pendingLoad: Pair<PlaybackSource, Long>? = null

    private val observer by lazy {
        object : MPVLib.EventObserver {
            override fun event(@MPVLib.Event eventID: Int) {
                when (eventID) {
                    MPVLib.MPV_EVENT_SEEK -> {
                        updateStatus(PlaybackStatus.Seeking)
                    }

                    MPVLib.MPV_EVENT_PLAYBACK_RESTART -> {
                        if (!playerInitialized) {
                            playerInitialized = true
                            MPVLib.setPropertyBoolean("pause", false)
                        }
                    }

                    else -> Unit
                }
            }

            override fun eventProperty(p0: String) {}

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
                    "media-title" -> sink.onMediaTitle(value)
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
            MPVLib.setPropertyBoolean("pause", !playing)
        }
    }

    override fun seek(position: Long) {
        if (playerInitialized) {
            MPVLib.command(arrayOf("set", "time-pos", "$position"))
        }
    }

    override fun setSubtitleTrack(id: Int) {
        if (canChangeTrack()) {
            MPVLib.command(arrayOf("set", "sub", if (id == -1) "no" else "$id"))
        }
    }

    override fun setAudioTrack(id: Int) {
        if (canChangeTrack()) {
            MPVLib.command(arrayOf("set", "audio", "$id"))
        }
    }

    override fun showMessage(message: String, durationMillis: Int) {
        if (libWasLoaded) {
            MPVLib.command(arrayOf("show-text", message, "$durationMillis"))
        }
    }

    override fun release() {
        runCatching {
            if (libWasLoaded) {
                MPVLib.removeObserver(observer)
                MPVLib.destroy()
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

        MPVLib.create(context)
        setMPVOptions(context, preferences)
        MPVLib.init()
        initObservers()
        libWasLoaded = true
        pendingLoad?.let { (source, startAt) ->
            pendingLoad = null
            load(source, startAt)
        }
    }

    private fun loadFile(pathOrUrl: String, startAt: Long) {
        MPVLib.command(arrayOf("set", "start", "$startAt"))
        MPVLib.command(arrayOf("loadfile", pathOrUrl, "replace"))
    }

    private fun initObservers() {
        MPVLib.addObserver(observer)
        arrayOf(
            Property("track-list", MPVLib.MPV_FORMAT_STRING),
            Property("chapter-list", MPVLib.MPV_FORMAT_STRING),
            Property("paused-for-cache", MPVLib.MPV_FORMAT_FLAG),
            Property("eof-reached", MPVLib.MPV_FORMAT_FLAG),
            Property("seekable", MPVLib.MPV_FORMAT_FLAG),
            Property("time-pos", MPVLib.MPV_FORMAT_INT64),
            Property("percent-pos", MPVLib.MPV_FORMAT_DOUBLE),
            Property("duration", MPVLib.MPV_FORMAT_INT64),
            Property("pause", MPVLib.MPV_FORMAT_FLAG),
            Property("seeking", MPVLib.MPV_FORMAT_FLAG),
            Property("demuxer-cache-time", MPVLib.MPV_FORMAT_INT64),
            Property("media-title", MPVLib.MPV_FORMAT_STRING)
        ).forEach { (name, format) ->
            MPVLib.observeProperty(name, format)
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
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir.toString())
        MPVLib.setOptionString("profile", pref.getPlatformProfile())
        if (pref.profile == "normal") {
            MPVLib.setOptionString("scale", "catmull_rom")
            MPVLib.setOptionString("cscale", "bilinear")
            MPVLib.setOptionString("dscale", "bilinear")
        }

        MPVLib.setOptionString("dither", if (pref.profile == "high") "fruit" else "ordered")
        MPVLib.setOptionString("gpu-api", pref.gpuAPI)
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")
        MPVLib.setOptionString("tls-verify", "no")
        MPVLib.setOptionString("blend-subtitles", "yes")
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("demuxer-max-bytes", "50MiB")
        MPVLib.setOptionString("demuxer-max-back-bytes", "32MiB")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("keep-open", "always")
        MPVLib.setOptionString("ytdl", "no")
        MPVLib.setOptionString("save-position-on-quit", "no")
        MPVLib.setOptionString("sub-font-provider", "none")
        MPVLib.setOptionString(
            "hwdec", when (settings["hwdec", "copy"]) {
                "yes" -> "mediacodec"
                "copy" -> "mediacodec-copy"
                else -> "no"
            }
        )
        MPVLib.setOptionString("hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        MPVLib.setOptionString("deband", if (pref.deband) "yes" else "no")
        MPVLib.setOptionString("deband-iterations", pref.debandIterations)
        MPVLib.setOptionString("dither-depth", if (pref.dither10bit) "10" else "8")
        MPVLib.setOptionString("slang", pref.getSlangArg(preferences))
        MPVLib.setOptionString("alang", pref.getAlangArg(preferences))
    }

    private fun surfaceHolderCallback(): SurfaceHolder.Callback {
        return object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                MPVLib.attachSurface(holder.surface)
                MPVLib.setOptionString("force-window", "yes")
                MPVLib.setOptionString("vo", MpvPreferences.getOrDefault().videoOutputDriver)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int,
            ) {
                MPVLib.setPropertyString("android-surface-size", "${width}x$height")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                MPVLib.setOptionString("vo", "null")
                MPVLib.setOptionString("force-window", "no")
                MPVLib.detachSurface()
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

private data class Property(val name: String, @MPVLib.Format val format: Int)
