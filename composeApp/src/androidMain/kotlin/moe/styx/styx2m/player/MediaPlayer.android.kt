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

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) : AMediaPlayer(initialEntryID, startAt) {
    val observer by lazy {
        object : MPVLib.EventObserver {
            override fun event(p0: Int) {}
            override fun eventProperty(p0: String) {}

            override fun eventProperty(property: String, value: Long) {
            }

            override fun eventProperty(property: String, value: Double) {
                Log.d("MPV") { "Double property changed - $property: $value" }
            }

            override fun eventProperty(property: String, value: Boolean) {
                when (property) {
                    in arrayOf("pause", "paused-for-cache") -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            isPlaying.emit(!value)
                        }
                    }

                    else -> Log.d("MPV") { "Bool property changed - $property: $value" }
                }
            }

            override fun eventProperty(property: String, value: String) {
                Log.d("MPV") { "String property changed - $property: $value" }
            }
        }
    }
    private var playerInitialized = false

    private fun initObservers() {
        MPVLib.addObserver(observer)
        arrayOf(
            Property("track-list", MPVLib.MPV_FORMAT_STRING),
            Property("paused-for-cache", MPVLib.MPV_FORMAT_FLAG),
            Property("eof-reached", MPVLib.MPV_FORMAT_FLAG),
            Property("seekable", MPVLib.MPV_FORMAT_FLAG),
            Property("time-pos", MPVLib.MPV_FORMAT_INT64),
            Property("duration", MPVLib.MPV_FORMAT_INT64),
            Property("pause", MPVLib.MPV_FORMAT_FLAG),
            Property("demuxer-cache-time", MPVLib.MPV_FORMAT_INT64),
            Property("speed", MPVLib.MPV_FORMAT_DOUBLE),
        ).forEach { (name, format) ->
            MPVLib.observeProperty(name, format)
        }
    }

    override fun setPlaying(playing: Boolean) {
        if (playerInitialized) {
            MPVLib.command(arrayOf("set", "pause", if (playing) "no" else "yes"))
        }
    }

    @Composable
    override fun PlayerComponent() {
        val context = LocalContext.current
        val entryList by Storage.stores.entryStore.getCurrentAndCollectFlow()
        val curEntryID by this.currentEntry.collectAsState()
        val currentEntry = entryList.find { it.GUID eqI curEntryID }
        if (!playerInitialized) {
            MPVLib.create(context)
            setMPVOptions()
            MPVLib.init()
            initObservers()
            playerInitialized = true
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
            MPVLib.setOptionString("vo", "gpu")
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

private fun setMPVOptions() {
    MPVLib.setOptionString("config", "no")
    MPVLib.setOptionString("profile", "fast")
    MPVLib.setOptionString("gpu-context", "android")
    MPVLib.setOptionString("opengl-es", "yes")
    MPVLib.setOptionString("tls-verify", "no")
    MPVLib.setOptionString("cache", "yes")
    MPVLib.setOptionString("demuxer-max-bytes", "32MiB")
    MPVLib.setOptionString("demuxer-max-back-bytes", "32MiB")
    MPVLib.setOptionString("force-window", "no")
    MPVLib.setOptionString("keep-open", "always")
    MPVLib.setOptionString("save-position-on-quit", "no")
    MPVLib.setOptionString("sub-font-provider", "none")
}

data class Property(val name: String, @MPVLib.Format val format: Int)