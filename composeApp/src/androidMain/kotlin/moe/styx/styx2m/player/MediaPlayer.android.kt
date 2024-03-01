package moe.styx.styx2m.player

import Styx_m.composeApp.BuildConfig
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.jdtech.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.http.login
import moe.styx.common.compose.utils.Log
import moe.styx.common.extension.eqI
import moe.styx.common.json

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) : AMediaPlayer(initialEntryID, startAt) {
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
                        playbackPercent = value
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
                            scope.launch { if (value) playbackStatus.emit(PlaybackStatus.Paused) else playbackStatus.emit(PlaybackStatus.Playing) }
                        }
                    }

                    "paused-for-cache" -> {
                        if (playerInitialized)
                            scope.launch { if (value) playbackStatus.emit(PlaybackStatus.Buffering) else playbackStatus.emit(PlaybackStatus.Playing) }
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

    @Composable
    override fun PlayerComponent() {
        val context = LocalContext.current
        val entryList by Storage.stores.entryStore.getCurrentAndCollectFlow()
        val curEntryID by this.currentEntry.collectAsState()
        val currentEntry = entryList.find { it.GUID eqI curEntryID }
        initialCommands = listOf(
            arrayOf("set", "start", "$startAt"),
//            arrayOf("loadfile", "${BuildConfig.BASE_URL}/watch/${currentEntry?.GUID}?token=${login?.watchToken}", "append")
        )
        if (!playerInitialized && !libWasLoaded) {
            MPVLib.create(context)
            setMPVOptions()
            MPVLib.init()
            initObservers()
            libWasLoaded = true
        }
        AndroidView(factory = { ctx ->
            val view = SurfaceView(ctx)
            view.holder.addCallback(surfaceHolder)
            view
        }, Modifier.fillMaxSize())

        LaunchedEffect(currentEntry) {
            if (currentEntry != null && login != null) {
                MPVLib.command(arrayOf("loadfile", "${BuildConfig.BASE_URL}/watch/${currentEntry.GUID}?token=${login?.watchToken}", "append-play"))
            }
        }
    }

    override fun releasePlayer() {
        runCatching {
            MPVLib.removeObserver(observer)
            MPVLib.destroy()
        }
    }

    private val surfaceHolder: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            MPVLib.attachSurface(holder.surface)
            MPVLib.setOptionString("force-window", "yes")
            MPVLib.setOptionString("vo", "gpu-next")
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

private fun MediaPlayer.setMPVOptions() {
    MPVLib.setOptionString("config", "no")
    MPVLib.setOptionString("profile", "default")
    MPVLib.setOptionString("gpu-api", "vulkan")
    MPVLib.setOptionString("gpu-context", "android")
    MPVLib.setOptionString("opengl-es", "yes")
    MPVLib.setOptionString("tls-verify", "no")
    MPVLib.setOptionString("cache", "yes")
    MPVLib.setOptionString("demuxer-max-bytes", "32MiB")
    MPVLib.setOptionString("demuxer-max-back-bytes", "32MiB")
    MPVLib.setOptionString("force-window", "no")
    MPVLib.setOptionString("keep-open", "always")
    MPVLib.setOptionString("ytdl", "no")
    MPVLib.setOptionString("save-position-on-quit", "no")
    MPVLib.setOptionString("sub-font-provider", "none")
    if (this.startAt != 0L) {
        MPVLib.setOptionString("start", "$startAt")
    }
}

data class Property(val name: String, @MPVLib.Format val format: Int)