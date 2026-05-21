package moe.styx.styx2m.player

import io.github.xxfast.kstore.extensions.getOrEmpty
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.http.Endpoints
import moe.styx.common.compose.http.login
import moe.styx.common.compose.utils.ServerStatus
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.eqI

object PlaybackSourceResolver {
    suspend fun resolve(entryID: String, entryList: List<MediaEntry>): PlaybackSource {
        val downloaded = Storage.stores.downloadedStore.getOrEmpty().find { it.entryID eqI entryID }
        if (downloaded != null) {
            return PlaybackSource.LocalFile(downloaded.path)
        }

        if (ServerStatus.lastKnown == ServerStatus.UNKNOWN || login == null) {
            return PlaybackSource.Unavailable(
                "You do not have this episode downloaded and are not logged in.\nOr don't have a working connection..."
            )
        }

        val currentEntry = entryList.find { it.GUID eqI entryID }
            ?: return PlaybackSource.Unavailable("Could not find the selected episode.")

        return PlaybackSource.RemoteUrl("${Endpoints.WATCH.url()}/${currentEntry.GUID}?token=${login?.watchToken}")
    }
}
