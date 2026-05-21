package moe.styx.styx2m.player

enum class PlayerTrackType {
    AUDIO,
    SUBTITLE,
    VIDEO,
    UNKNOWN
}

data class PlayerTrack(
    val id: Int,
    val type: PlayerTrackType,
    val title: String? = null,
    val language: String? = null,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isSelected: Boolean = false,
    val codec: String? = null,
    val channels: Int? = null
)
