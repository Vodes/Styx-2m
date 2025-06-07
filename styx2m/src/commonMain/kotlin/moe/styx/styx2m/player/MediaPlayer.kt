package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import moe.styx.common.compose.extensions.joinAndSyncProgress
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaPreferences
import moe.styx.common.util.Log
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
    var mainVm: MainDataViewModel? = null

    abstract fun setPlaying(playing: Boolean)
    abstract fun seek(position: Long)
    abstract fun setSubtitleTrack(id: Int)
    abstract fun setAudioTrack(id: Int)

    @Composable
    abstract fun requestRotationLock()

    @Composable
    abstract fun releaseRotationLock()

    fun playEntry(mediaEntry: MediaEntry, scope: CoroutineScope) {
        updateWatchedForID(currentEntry.value, progress.value, playbackPercent)
        if (mainVm != null) {
            val mainVm = mainVm!!
            mainVm.updateData().joinAndSyncProgress(currentEntry.value, mainVm)
        } else
            Log.w(source = "MediaPlayer") { "MainVM was null!" }
        internalPlayEntry(mediaEntry, scope)
    }

    abstract fun internalPlayEntry(mediaEntry: MediaEntry, scope: CoroutineScope)

    @Composable
    abstract fun PlayerComponent(entryList: List<MediaEntry>, preferences: MediaPreferences?)

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

data class PlayerState(
    val mediaTitle: String = "",
    val cacheEnd: Long = 0L,
    val progress: Long = 0L,
    val fileLength: Long = 0L,
    val trackList: List<Track> = emptyList(),
    val chapters: List<Chapter> = emptyList()
)

expect class MediaPlayer(initialEntryID: String, startAt: Long = 0L) : AMediaPlayer {
    override fun releasePlayer()
    override fun seek(position: Long)
    override fun setAudioTrack(id: Int)
    override fun setSubtitleTrack(id: Int)
    override fun setPlaying(playing: Boolean)
    override fun internalPlayEntry(mediaEntry: MediaEntry, scope: CoroutineScope)

    @Composable
    override fun requestRotationLock()

    @Composable
    override fun releaseRotationLock()

    @Composable
    override fun PlayerComponent(entryList: List<MediaEntry>, preferences: MediaPreferences?)
}