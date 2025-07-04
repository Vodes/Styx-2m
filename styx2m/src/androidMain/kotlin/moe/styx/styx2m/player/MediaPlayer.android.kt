package moe.styx.styx2m.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.russhwolf.settings.get
import dev.jdtech.mpv.MPVLib
import io.github.xxfast.kstore.extensions.getOrEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.styx.common.compose.AppContextImpl.appConfig
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.http.Endpoints
import moe.styx.common.compose.http.login
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.MpvPreferences
import moe.styx.common.compose.utils.ServerStatus
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaPreferences
import moe.styx.common.extension.eqI
import moe.styx.common.json
import moe.styx.common.util.Log
import moe.styx.common.util.SYSTEMFILES
import moe.styx.styx2m.R
import moe.styx.styx2m.misc.findActivity
import okio.Path.Companion.toPath
import java.io.FileOutputStream

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) :
    AMediaPlayer(initialEntryID, startAt) {
    val observer by lazy {
        object : MPVLib.EventObserver {
            override fun event(@MPVLib.Event eventID: Int) {
                val scope = CoroutineScope(Dispatchers.Main)
                when (eventID) {
                    MPVLib.MPV_EVENT_SEEK -> {
                        scope.launch { playbackStatus.emit(PlaybackStatus.Seeking) }
                    }

                    MPVLib.MPV_EVENT_START_FILE -> {
                        if (!playerInitialized) {
                            initialCommands.forEach {
                                MPVLib.command(it)
                            }
                        }
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
                val scope = CoroutineScope(Dispatchers.Main)
                when (property) {
                    "time-pos" -> scope.launch { progress.emit(value) }
                    "duration" -> scope.launch { fileLength.emit(value) }
                    "demuxer-cache-time" -> scope.launch { cacheEnd.emit(value) }

                    else -> Log.d("MPV") { "Long property changed - $property: $value" }
                }
            }

            override fun eventProperty(property: String, value: Double) {
                when (property) {
                    "percent-pos" -> {
                        playbackPercent = value.toFloat()
                    }

                    else -> Log.d("MPV") { "Double property changed - $property: $value" }
                }
            }

            override fun eventProperty(property: String, value: Boolean) {
                val scope = CoroutineScope(Dispatchers.Main)
                when (property) {
                    "pause" -> {
                        isPaused = value
                        if (playbackStatus.value !is PlaybackStatus.Buffering) {
                            scope.launch {
                                if (value) playbackStatus.emit(PlaybackStatus.Paused) else playbackStatus.emit(
                                    PlaybackStatus.Playing
                                )
                            }
                        }
                    }

                    "paused-for-cache" -> {
                        if (playerInitialized)
                            scope.launch {
                                if (value) playbackStatus.emit(PlaybackStatus.Buffering) else playbackStatus.emit(
                                    PlaybackStatus.Playing
                                )
                            }
                    }

                    "seeking" -> {
                        if (playbackStatus.value !is PlaybackStatus.Buffering) {
                            scope.launch {
                                if (value) playbackStatus.emit(PlaybackStatus.Seeking)
                                else playbackStatus.emit(if (isPaused) PlaybackStatus.Paused else PlaybackStatus.Playing)
                            }
                        }
                    }

                    else -> Log.d("MPV") { "Bool property changed - $property: $value" }
                }
            }

            override fun eventProperty(property: String, value: String) {
                val scope = CoroutineScope(Dispatchers.Main)
                when (property) {
                    "media-title" -> scope.launch { mediaTitle.emit(value) }
                    "track-list" -> {
                        scope.launch {
                            runCatching {
                                trackList.emit(json.decodeFromString(value))
                            }.onFailure {
                                Log.e("MPV", it) { "Failed to parse track-list!" }
                            }
                        }
                    }

                    "chapter-list" -> {
                        scope.launch {
                            runCatching {
                                chapters.emit(json.decodeFromString(value))
                            }
                        }
                    }

                    else -> Log.d("MPV") { "String property changed - $property: $value" }
                }
            }
        }
    }
    var playerInitialized = false
    var libWasLoaded = false
    var initialCommands: List<Array<String>> = emptyList()

    actual override fun setPlaying(playing: Boolean) {
        if (playerInitialized) {
            MPVLib.setPropertyBoolean("pause", !playing)
        }
    }

    actual override fun seek(position: Long) {
        if (playerInitialized) {
            MPVLib.command(arrayOf("set", "time-pos", "$position"))
        }
    }

    @Composable
    actual override fun requestRotationLock() {
        val context = LocalContext.current
        val activity = context.findActivity()
        LaunchedEffect(Unit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            Log.d { "Requesting locked rotation: ${activity != null}" }
        }
        DisposableEffect(Unit) {
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                Log.d { "Releasing locked rotation: ${activity != null}" }
            }
        }
    }

    @Composable
    actual override fun releaseRotationLock() {
        val context = LocalContext.current
        val activity = context.findActivity()
        LaunchedEffect(Unit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            Log.d { "Releasing locked rotation: ${activity != null}" }
        }
    }

    actual override fun setSubtitleTrack(id: Int) {
        if (playerInitialized && (playbackStatus.value in arrayOf(PlaybackStatus.Playing, PlaybackStatus.Paused))) {
            MPVLib.command(arrayOf("set", "sub", if (id == -1) "no" else "$id"))
        }
    }

    actual override fun setAudioTrack(id: Int) {
        if (playerInitialized && (playbackStatus.value in arrayOf(PlaybackStatus.Playing, PlaybackStatus.Paused))) {
            MPVLib.command(arrayOf("set", "audio", "$id"))
        }
    }

    actual override fun internalPlayEntry(mediaEntry: MediaEntry, scope: CoroutineScope) {
        if (!playerInitialized)
            return
        MPVLib.command(arrayOf("set", "start", "0"))
        initialCommands = emptyList()
        scope.launch { currentEntry.emit(mediaEntry.GUID) }
    }

    @Composable
    actual override fun PlayerComponent(entryList: List<MediaEntry>, preferences: MediaPreferences?) {
        val context = LocalContext.current
        val curEntryID by this.currentEntry.collectAsState()
        initialCommands = listOf(
            arrayOf("set", "start", "$startAt"),
        )
        if (!playerInitialized && !libWasLoaded) {
            MPVLib.create(context)
            setMPVOptions(context, preferences)
            MPVLib.init()
            initObservers()
            libWasLoaded = true
        }
        AndroidView(factory = { ctx ->
            val view = SurfaceView(ctx)
            view.holder.addCallback(surfaceHolder)
            view
        }, Modifier.fillMaxSize())

        LaunchedEffect(curEntryID) {
            val downloaded = Storage.stores.downloadedStore.getOrEmpty().find { it.entryID eqI curEntryID }
            val currentEntry = entryList.find { it.GUID eqI curEntryID }
            if ((ServerStatus.lastKnown == ServerStatus.UNKNOWN || login == null) && downloaded == null) {
                MPVLib.command(
                    arrayOf(
                        "show-text",
                        "You do not have this episode downloaded and are not logged in.\nOr don't have a working connection...",
                        "5000"
                    )
                )
                return@LaunchedEffect
            }
            if (currentEntry != null) {
                // "Fix" for downloaded files, stolen from findroid
                withContext(Dispatchers.IO) {
                    Thread.sleep(1)
                }
                if (downloaded != null) {
                    MPVLib.command(arrayOf("loadfile", downloaded.path, "replace"))
                } else {
                    MPVLib.command(
                        arrayOf(
                            "loadfile",
                            "${Endpoints.WATCH.url()}/${currentEntry.GUID}?token=${login?.watchToken}",
                            "replace"
                        )
                    )
                }
            }
        }
    }

    actual override fun releasePlayer() {
        runCatching {
            MPVLib.removeObserver(observer)
            MPVLib.destroy()
        }
    }

    private val surfaceHolder: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
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

private fun MediaPlayer.initObservers() {
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

private fun MediaPlayer.setMPVOptions(context: Context, preferences: MediaPreferences?) {
    val configDir = appConfig().appStoragePath.toPath() / "mpv"
    SYSTEMFILES.createDirectory(configDir)
    if ((SYSTEMFILES.listOrNull(configDir) ?: emptyList()).find { it.name eqI "subfont.ttf" } == null) {
        runCatching {
            val fontStream = context.resources.openRawResource(R.raw.subfont)
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

    MPVLib.setOptionString("gpu-api", pref.gpuAPI) // Offers vulkan but doesn't seem to do anything
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
    if (this.startAt != 0L) {
        MPVLib.setOptionString("start", "$startAt")
    }
}

data class Property(val name: String, @MPVLib.Format val format: Int)