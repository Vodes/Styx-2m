package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow

abstract class AMediaPlayer(val initialEntryID: String, val startAt: Long = 0L) {
    val currentEntry = MutableStateFlow(initialEntryID)
    val cacheEnd = MutableStateFlow(0L)
    val progress = MutableStateFlow(0L)
    val fileLength = MutableStateFlow(0L)
    val playbackStatus = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
    var playbackPercent = 0.0

    abstract fun setPlaying(playing: Boolean)
    abstract fun seek(position: Long)

    @Composable
    abstract fun PlayerComponent()

    abstract fun releasePlayer()
}

sealed class PlaybackStatus {
    data object Idle : PlaybackStatus()
    data object Playing : PlaybackStatus()
    data object Paused : PlaybackStatus()
    data object EOF : PlaybackStatus()
    data object Buffering : PlaybackStatus()
    data object Seeking : PlaybackStatus()
}

expect class MediaPlayer(initialEntryID: String, startAt: Long = 0L) : AMediaPlayer