package moe.styx.styx2m.player

sealed interface PlaybackSource {
    data class LocalFile(val path: String) : PlaybackSource
    data class RemoteUrl(val url: String) : PlaybackSource
    data class Unavailable(val message: String) : PlaybackSource
}
