package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaPreferences

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) : AMediaPlayer(initialEntryID, startAt) {
    actual override fun setPlaying(playing: Boolean) {
        TODO("Not yet implemented")
    }

    actual override fun seek(position: Long) {
        TODO("Not yet implemented")
    }

    actual override fun setSubtitleTrack(id: Int) {
        TODO("Not yet implemented")
    }

    actual override fun setAudioTrack(id: Int) {
        TODO("Not yet implemented")
    }

    actual override fun internalPlayEntry(mediaEntry: MediaEntry, scope: CoroutineScope) {
        TODO("Not yet implemented")
    }

    @Composable
    actual override fun requestRotationLock() = Unit

    @Composable
    actual override fun releaseRotationLock() = Unit

    @Composable
    actual override fun PlayerComponent(entryList: List<MediaEntry>, preferences: MediaPreferences?) {
        TODO("Not yet implemented")
    }

    actual override fun releasePlayer() {
        TODO("Not yet implemented")
    }
}
