package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.jdtech.mpv.MPVLib

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) : AMediaPlayer(initialEntryID, startAt) {

    class MpvObserver : MPVLib.EventObserver {
        override fun eventProperty(p0: String) {
            TODO("Not yet implemented")
        }

        override fun eventProperty(p0: String, p1: Long) {
            TODO("Not yet implemented")
        }

        override fun eventProperty(p0: String, p1: Double) {
            TODO("Not yet implemented")
        }

        override fun eventProperty(p0: String, p1: Boolean) {
            TODO("Not yet implemented")
        }

        override fun eventProperty(p0: String, p1: String) {
            TODO("Not yet implemented")
        }

        override fun event(p0: Int) {
            TODO("Not yet implemented")
        }

    }

    @Composable
    override fun PlayerComponent() {
        val context = LocalContext.current
        MPVLib.create(context)
    }
}