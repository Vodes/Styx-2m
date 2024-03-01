package moe.styx.styx2m.misc

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import io.islandtime.measures.hours
import io.islandtime.measures.seconds
import moe.styx.common.extension.padString

inline fun Float.ifInvalid(newValue: Float): Float {
    if (this.isNaN() || this.isInfinite())
        return newValue
    return this
}

fun Long.secondsDurationString(): String {
    val seconds = this.seconds
    return seconds.toComponents { hours, minutes, sec ->
        if (hours < 1.hours) {
            "${minutes.value.toInt().padString(2)}:${sec.value.toInt().padString(2)}"
        } else
            "${hours.value.toInt().padString(2)}${minutes.value.toInt().padString(2)}:${sec.value.toInt().padString(2)}"
    }
}

val otherAppShape
    get() = RoundedCornerShape(6.dp)