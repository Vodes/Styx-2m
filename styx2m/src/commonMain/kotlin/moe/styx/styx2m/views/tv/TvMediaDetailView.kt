package moe.styx.styx2m.views.tv

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.russhwolf.settings.get
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.anime.*
import moe.styx.common.compose.components.buttons.FavouriteIconButton
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.extensions.joinAndSyncProgress
import moe.styx.common.compose.extensions.readableSize
import moe.styx.common.compose.extensions.removeSomeHTMLTags
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.RequestQueue
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.LocalToaster
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.common.compose.viewmodels.MainDataViewModelStorage
import moe.styx.common.compose.viewmodels.MediaStorage
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.misc.getProgress
import moe.styx.styx2m.misc.pushMediaView
import moe.styx.styx2m.player.PlayerView

class TvMediaDetailView(private val mediaID: String) : Screen {

    override val key: ScreenKey
        get() = mediaID

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val toaster = LocalToaster.current
        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        val storage by sm.storageFlow.collectAsState()
        val mediaStorage = remember(storage) { sm.getMediaStorageForID(mediaID, storage) }

        MainScaffold(title = mediaStorage.media.name, actions = {
            MediaPreferencesIconButton(mediaStorage.preferences, mediaStorage.media, sm)
            FavouriteIconButton(mediaStorage.media, sm, storage)
        }) {
            val showSelection = remember { mutableStateOf(false) }
            Column(Modifier.padding(5.dp)) {
                if (mediaStorage.media.isSeries.toBoolean()) {
                    EpisodeList(storage, mediaStorage, showSelection, null, canShowMediaInfo = false, onPlay = { entry ->
                        nav.push(PlayerView(entry.GUID, storage.watchedList.find { wat -> wat.entryID eqI entry.GUID }.getProgress()))
                        ""
                    }, headerContent = {
                        MetadataArea(mediaStorage, storage, sm)
                    })
                } else {
                    MetadataArea(mediaStorage, storage, sm)
                }
            }
        }
    }

    @Composable
    private fun PlayRow(mediaStorage: MediaStorage, storage: MainDataViewModelStorage, sm: MainDataViewModel) {
        val nav = LocalGlobalNavigator.current
        Row(Modifier.padding(3.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton({
                if (!mediaStorage.media.isSeries.toBoolean()) {
                    val entry = mediaStorage.entries.first()
                    nav.push(PlayerView(entry.GUID, storage.watchedList.find { wat -> wat.entryID eqI entry.GUID }.getProgress()))
                }
                val mapped = mediaStorage.entries.map { ent -> ent to storage.watchedList.find { it.entryID eqI ent.GUID } }
                val lastWatched = mapped.filterNot { it.second == null }.maxByOrNull { it.first.entryNumber.toDoubleOrNull() ?: 0.0 }
                val nextUnwatched =
                    mapped.filter {
                        it.second == null
                                && (it.first.entryNumber.toDoubleOrNull() ?: 0.0) > (lastWatched?.first?.entryNumber?.toDoubleOrNull() ?: 0.0)
                    }.minByOrNull {
                        it.first.entryNumber.toDoubleOrNull() ?: 0.0
                    }
                if (lastWatched != null) {
                    if (lastWatched.second!!.maxProgress < 85F) {
                        nav.push(PlayerView(lastWatched.first.GUID, lastWatched.second.getProgress()))
                        return@IconButton
                    }
                }
                if (nextUnwatched != null) {
                    nav.push(PlayerView(nextUnwatched.first.GUID, nextUnwatched.second.getProgress()))
                }
            }) {
                Icon(Icons.Filled.PlayArrow, "Start playing")
            }

            IconButton(onClick = {
                val job = RequestQueue.addMultipleWatched(mediaStorage.entries)
                sm.screenModelScope.launch {
                    job.join()
                    sm.updateData(true).joinAndSyncProgress(mediaStorage.entries.maxByOrNull { it.entryNumber }!!, sm)
                }
            }) { Icon(Icons.Default.Visibility, "Set Watched") }

            IconButton(onClick = {
                RequestQueue.removeMultipleWatched(mediaStorage.entries)
            }) { Icon(Icons.Default.VisibilityOff, "Set Unwatched") }

            Spacer(Modifier.weight(1f))
            Text(
                if (!mediaStorage.media.isSeries.toBoolean()) mediaStorage.entries.first().fileSize.readableSize() else "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Composable
    private fun MetadataArea(mediaStorage: MediaStorage, storage: MainDataViewModelStorage, sm: MainDataViewModel) {
        val nav = LocalGlobalNavigator.current
        StupidImageNameArea(
            mediaStorage,
            requiredMaxHeight = 535.dp,
            mappingIconModifier = Modifier.padding(8.dp, 5.dp, 8.dp, 12.dp).size(30.dp),
            enforceConstraints = true,
            showMappingIcons = false,
        ) {
            Text("About", Modifier.padding(6.dp, 2.dp), style = MaterialTheme.typography.titleLarge)
            MediaGenreListing(mediaStorage.media)
            val preferGerman = settings["prefer-german-metadata", false]
            val synopsis =
                if (!mediaStorage.media.synopsisDE.isNullOrBlank() && preferGerman) mediaStorage.media.synopsisDE else mediaStorage.media.synopsisEN
            if (!synopsis.isNullOrBlank())
                Text(synopsis.removeSomeHTMLTags(), Modifier.padding(6.dp), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            PlayRow(mediaStorage, storage, sm)
            Spacer(Modifier.height(5.dp))
        }
        if (mediaStorage.sequel != null || mediaStorage.prequel != null)
            MediaRelations(mediaStorage) {
                nav.pushMediaView(it)
            }
        if (mediaStorage.media.isSeries.toBoolean()) {
            HorizontalDivider(Modifier.fillMaxWidth().padding(5.dp))
            Spacer(Modifier.height(4.dp))
        }
    }
}