package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow

abstract class AMediaPlayer(val initialEntryID: String, val startAt: Long = 0L) {
    val currentEntry = MutableStateFlow(initialEntryID)
    val progress = MutableStateFlow(0L)
    val fileLength = MutableStateFlow(0L)
    val isPlaying = MutableStateFlow(false)

    @Composable
    abstract fun PlayerComponent()
}

expect class MediaPlayer(initialEntryID: String, startAt: Long = 0L) : AMediaPlayer