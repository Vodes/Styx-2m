package moe.styx.styx2m.misc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: Int,
    @SerialName("src-id")
    val absoluteId: Int,
    val type: String,
    val title: String,
    val lang: String,
    val default: Boolean,
    val forced: Boolean,
    val selected: Boolean,
    val codec: String? = null,
    @SerialName("audio-channels")
    val audioChannels: Int? = null
)

@Serializable
data class Chapter(val title: String, val time: Float)