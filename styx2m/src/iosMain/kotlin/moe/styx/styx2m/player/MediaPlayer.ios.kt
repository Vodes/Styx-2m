package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import moe.styx.common.data.MediaEntry

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) : AMediaPlayer(initialEntryID, startAt) {
    override fun setPlaying(playing: Boolean) {
        TODO("Not yet implemented")
    }

    override fun seek(position: Long) {
        TODO("Not yet implemented")
    }

    override fun setSubtitleTrack(id: Int) {
        TODO("Not yet implemented")
    }

    override fun setAudioTrack(id: Int) {
        TODO("Not yet implemented")
    }

    override fun internalPlayEntry(mediaEntry: MediaEntry, scope: CoroutineScope) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun PlayerComponent(entryList: List<MediaEntry>) {
        TODO("Not yet implemented")
    }

    override fun releasePlayer() {
        TODO("Not yet implemented")
    }
}