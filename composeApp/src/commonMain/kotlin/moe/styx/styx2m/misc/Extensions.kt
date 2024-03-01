package moe.styx.styx2m.misc

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.set
import io.islandtime.measures.hours
import io.islandtime.measures.seconds
import kotlinx.serialization.encodeToString
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.MpvPreferences
import moe.styx.common.extension.padString
import moe.styx.common.json

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

fun MpvPreferences.save(): MpvPreferences {
    settings["mpv-preferences"] = json.encodeToString(this)
    return this
}

val otherAppShape
    get() = RoundedCornerShape(6.dp)