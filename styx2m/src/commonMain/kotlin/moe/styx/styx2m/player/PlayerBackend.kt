package moe.styx.styx2m.player

import moe.styx.styx2m.misc.Chapter

enum class PlayerBackendId {
    MPV,
    VLC
}

data class PlayerBackendCapabilities(
    val supportsCacheEnd: Boolean = false,
    val supportsChapters: Boolean = false,
    val supportsAudioTrackSelection: Boolean = true,
    val supportsSubtitleTrackSelection: Boolean = true,
    val supportsLocalFiles: Boolean = true,
    val supportsHttpStreams: Boolean = true
)

interface PlayerBackend {
    val id: PlayerBackendId
    val capabilities: PlayerBackendCapabilities

    fun load(source: PlaybackSource, startAt: Long)
    fun setPlaying(playing: Boolean)
    fun seek(position: Long)
    fun setAudioTrack(id: Int)
    fun setSubtitleTrack(id: Int)
    fun showMessage(message: String, durationMillis: Int = 5000)
    fun release()
}

interface PlayerBackendSink {
    fun onPlaybackStatus(status: PlaybackStatus)
    fun onMediaTitle(title: String)
    fun onProgress(seconds: Long)
    fun onDuration(seconds: Long)
    fun onCacheEnd(seconds: Long)
    fun onPlaybackPercent(percent: Float)
    fun onTracks(tracks: List<PlayerTrack>)
    fun onChapters(chapters: List<Chapter>)
    fun onError(source: String, message: String, throwable: Throwable? = null)
}
