package moe.styx.styx2m.player

import com.russhwolf.settings.get
import moe.styx.common.compose.settings
import moe.styx.common.util.Log
import moe.styx.styx2m.player.media3.Media3AndroidBackend
import moe.styx.styx2m.player.mpv.MpvAndroidBackend

actual fun createPlayerBackend(sink: PlayerBackendSink): PlayerBackendWithSurface {
    return when (val selectedBackend = settings["player-backend", "auto"]) {
        "auto", "mpv" -> MpvAndroidBackend(sink)
        "media3" -> Media3AndroidBackend(sink)
        else -> {
            Log.w("PlayerBackend") { "Unknown player backend '$selectedBackend', falling back to mpv." }
            MpvAndroidBackend(sink)
        }
    }
}
