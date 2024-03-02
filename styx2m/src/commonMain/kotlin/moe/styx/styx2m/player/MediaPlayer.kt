package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import moe.styx.common.data.MediaEntry
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.misc.Track
import moe.styx.styx2m.misc.updateWatchedForID

abstract class AMediaPlayer(val initialEntryID: String, val startAt: Long = 0L) {
    val currentEntry = MutableStateFlow(initialEntryID)
    val mediaTitle = MutableStateFlow("")
    val cacheEnd = MutableStateFlow(0L)
    val progress = MutableStateFlow(0L)
    val fileLength = MutableStateFlow(0L)
    val trackList = MutableStateFlow(emptyList<Track>())
    val chapters = MutableStateFlow(emptyList<Chapter>())
    val playbackStatus = MutableStateFlow<PlaybackStatus>(PlaybackStatus.Idle)
    var playbackPercent = 0.0F
    var isPaused = false

    abstract fun setPlaying(playing: Boolean)
    abstract fun seek(position: Long)
    abstract fun setSubtitleTrack(id: Int)
    abstract fun setAudioTrack(id: Int)

    fun playEntry(mediaEntry: MediaEntry, scope: CoroutineScope) {
        updateWatchedForID(currentEntry.value, progress.value, playbackPercent)
        internalPlayEntry(mediaEntry, scope)
    }

    abstract fun internalPlayEntry(mediaEntry: MediaEntry, scope: CoroutineScope)

    @Composable
    abstract fun PlayerComponent()

    abstract fun releasePlayer()
}

sealed class PlaybackStatus {
    data object Idle : PlaybackStatus()
    data object Ready : PlaybackStatus()
    data object Playing : PlaybackStatus()
    data object Paused : PlaybackStatus()
    data object EOF : PlaybackStatus()
    data object Buffering : PlaybackStatus()
    data object Seeking : PlaybackStatus()
}

expect class MediaPlayer(initialEntryID: String, startAt: Long = 0L) : AMediaPlayer