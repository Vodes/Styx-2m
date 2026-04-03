package moe.styx.styx2m.views.tv.comp

import moe.styx.common.compose.viewmodels.MainDataViewModelStorage
import moe.styx.common.compose.viewmodels.MediaStorage
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaWatched
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.misc.getProgress

internal fun MainDataViewModelStorage.findWatchedEntry(entry: MediaEntry): MediaWatched? {
    return watchedList.find { it.entryID eqI entry.GUID }
}

internal fun MainDataViewModelStorage.progressFor(entry: MediaEntry): Long {
    return findWatchedEntry(entry).getProgress()
}

internal fun MainDataViewModelStorage.unwatchedCount(mediaStorage: MediaStorage): Int {
    return mediaStorage.entries.count { (findWatchedEntry(it)?.maxProgress ?: 0F) < 85F }
}

internal fun MainDataViewModelStorage.entryToPlay(mediaStorage: MediaStorage): MediaEntry? {
    if (mediaStorage.entries.isEmpty()) {
        return null
    }

    if (!mediaStorage.media.isSeries.toBoolean()) {
        return mediaStorage.entries.first()
    }

    val mapped = mediaStorage.entries.map { it to findWatchedEntry(it) }
    val sortedByNumber = mapped.sortedBy { it.first.entryNumber.toDoubleOrNull() ?: 0.0 }
    val lastWatched = sortedByNumber.lastOrNull { it.second != null }

    if (lastWatched != null && (lastWatched.second?.maxProgress ?: 0F) < 85F) {
        return lastWatched.first
    }

    val nextUnwatched = sortedByNumber.firstOrNull { (entry, watched) ->
        watched == null && (entry.entryNumber.toDoubleOrNull() ?: 0.0) > (lastWatched?.first?.entryNumber?.toDoubleOrNull() ?: 0.0)
    }
    if (nextUnwatched != null) {
        return nextUnwatched.first
    }

    return lastWatched?.first ?: sortedByNumber.first().first
}

internal fun MainDataViewModelStorage.playLabel(mediaStorage: MediaStorage): String {
    val entry = entryToPlay(mediaStorage) ?: return "Open"
    if (!mediaStorage.media.isSeries.toBoolean()) {
        return "Play Movie"
    }

    val watched = findWatchedEntry(entry)
    return if (watched != null && watched.maxProgress < 85F) {
        "Resume Episode ${entry.entryNumber}"
    } else {
        "Play Episode ${entry.entryNumber}"
    }
}
