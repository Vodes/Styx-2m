package moe.styx.styx2m.misc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import moe.styx.styx2m.player.PlayerTrack
import moe.styx.styx2m.player.PlayerTrackType

@Serializable
data class Track(
    val id: Int,
    @SerialName("src-id")
    val absoluteId: Int,
    val type: String,
    val title: String? = null,
    val lang: String? = null,
    val default: Boolean,
    val forced: Boolean,
    val selected: Boolean,
    val codec: String? = null,
    @SerialName("audio-channels")
    val audioChannels: Int? = null
)

fun Track.toPlayerTrack(): PlayerTrack {
    val trackType = when (type.lowercase()) {
        "audio" -> PlayerTrackType.AUDIO
        "sub" -> PlayerTrackType.SUBTITLE
        "video" -> PlayerTrackType.VIDEO
        else -> PlayerTrackType.UNKNOWN
    }

    return PlayerTrack(
        id = id,
        type = trackType,
        title = title,
        language = lang,
        isDefault = default,
        isForced = forced,
        isSelected = selected,
        codec = codec,
        channels = audioChannels
    )
}

@Serializable
data class Chapter(val title: String, val time: Float)
