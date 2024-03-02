package moe.styx.styx2m.player

import androidx.compose.runtime.Composable

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) : AMediaPlayer(initialEntryID, startAt) {

    @Composable
    override fun PlayerComponent() {
        TODO("Not yet implemented")
    }
}