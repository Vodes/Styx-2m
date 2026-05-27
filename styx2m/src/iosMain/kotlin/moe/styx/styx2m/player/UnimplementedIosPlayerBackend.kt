package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import moe.styx.common.data.MediaPreferences

class UnimplementedIosPlayerBackend(
    override val id: PlayerBackendId
) : PlayerBackendWithSurface {
    override val capabilities = PlayerBackendCapabilities(
        supportsCacheEnd = false,
        supportsChapters = false,
        supportsAudioTrackSelection = false,
        supportsSubtitleTrackSelection = false,
        supportsLocalFiles = true,
        supportsHttpStreams = true
    )

    override fun load(source: PlaybackSource, startAt: Long) = Unit
    override fun setPlaying(playing: Boolean) = Unit
    override fun seek(position: Long) = Unit
    override fun setAudioTrack(id: Int) = Unit
    override fun setSubtitleTrack(id: Int) = Unit
    override fun showMessage(message: String, durationMillis: Int) = Unit
    override fun release() = Unit

    @Composable
    override fun RenderSurface(modifier: Modifier, preferences: MediaPreferences?) = Unit
}
