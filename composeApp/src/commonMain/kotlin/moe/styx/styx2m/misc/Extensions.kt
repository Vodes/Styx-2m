package moe.styx.styx2m.misc

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.set
import io.islandtime.measures.hours
import io.islandtime.measures.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import moe.styx.common.compose.http.login
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.RequestQueue
import moe.styx.common.compose.utils.MpvPreferences
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaWatched
import moe.styx.common.extension.currentUnixSeconds
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
            "${hours.value.toInt().padString(2)}:${minutes.value.toInt().padString(2)}:${sec.value.toInt().padString(2)}"
    }
}

fun List<MediaEntry>.findNext(currentEntry: MediaEntry, parentMedia: Media): MediaEntry? {
    val currentNum = currentEntry.entryNumber.toDoubleOrNull() ?: 0.0
    val entries =
        this.filter {
            val num = it.entryNumber.toDoubleOrNull() ?: 0.0
            it.mediaID == parentMedia.GUID && num > currentNum
        }
    return entries.minByOrNull { it.entryNumber.toDoubleOrNull() ?: 9999.0 }
}

fun List<MediaEntry>.findPrevious(currentEntry: MediaEntry, parentMedia: Media): MediaEntry? {
    val currentNum = currentEntry.entryNumber.toDoubleOrNull() ?: 0.0
    val entries =
        this.filter {
            val num = it.entryNumber.toDoubleOrNull() ?: 0.0
            it.mediaID == parentMedia.GUID && num < currentNum
        }
    return entries.maxByOrNull { it.entryNumber.toDoubleOrNull() ?: 9999.0 }
}

fun MediaWatched?.getProgress(): Long {
    val prog = this?.progress ?: 0
    if (prog < 10)
        return 0
    return prog - 5
}

fun updateWatchedForID(entryID: String, current: Long, percent: Float) = runBlocking {
    val watched = MediaWatched(entryID, login?.userID ?: "", currentUnixSeconds(), current, percent, percent)
    RequestQueue.updateWatched(watched).join()
    delay(200L)
}

fun MpvPreferences.save(): MpvPreferences {
    settings["mpv-preferences"] = json.encodeToString(this)
    return this
}

val otherAppShape
    get() = RoundedCornerShape(6.dp)