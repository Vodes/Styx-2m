package moe.styx.styx2m.views.anime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import com.russhwolf.settings.set
import moe.styx.common.compose.components.anime.*
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.extensions.joinAndSyncProgress
import moe.styx.common.compose.extensions.readableSize
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.collectWithEmptyInitial
import moe.styx.common.compose.files.updateList
import moe.styx.common.compose.http.login
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.DownloadQueue
import moe.styx.common.compose.threads.RequestQueue
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.common.compose.viewmodels.MediaStorage
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaWatched
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.eqI
import moe.styx.common.util.SYSTEMFILES
import moe.styx.common.util.launchThreaded
import moe.styx.styx2m.components.AboutView
import moe.styx.styx2m.misc.LayoutSizes
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.misc.getProgress
import moe.styx.styx2m.misc.pushMediaView
import moe.styx.styx2m.player.PlayerView

class MovieDetailView(private val mediaID: String) : Screen {

    override val key: ScreenKey
        get() = mediaID

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        val storage by sm.storageFlow.collectAsState()
        val mediaStorage = remember(storage) { sm.getMediaStorageForID(mediaID, storage) }
        val movieEntry = mediaStorage.entries.getOrNull(0)

        if (mediaStorage.image == null) {
            nav.pop()
            return
        }
        val sizes = LocalLayoutSize.current
        val watched = movieEntry?.let { storage.watchedList.find { it.entryID eqI movieEntry.GUID } }
        var showMediaInfoDialog by remember { mutableStateOf(false) }
        if (showMediaInfoDialog && movieEntry != null) {
            MediaInfoDialog(movieEntry) { showMediaInfoDialog = false }
        }
        MainScaffold(Modifier.fillMaxSize(), title = mediaStorage.media.name, actions = {
            MediaPreferencesIconButton(mediaStorage.preferences, mediaStorage.media, sm)
        }) {
            val scrollState = rememberScrollState()
            ElevatedCard(Modifier.fillMaxSize().padding(2.dp)) {
                if (sizes.isWide) {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(0.5F).verticalScroll(scrollState)) {
                            StupidImageNameArea(mediaStorage, requiredMaxHeight = 535.dp)
                            PlaycontrolRow(nav, false, movieEntry, watched, sm) { showMediaInfoDialog = !showMediaInfoDialog }
                        }
                        VerticalDivider(Modifier.fillMaxHeight().padding(10.dp), thickness = 3.dp)
                        Column(Modifier.weight(0.5F).padding(0.dp, 8.dp).verticalScroll(rememberScrollState())) {
                            MovieAboutView(mediaStorage, nav, sizes)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        StupidImageNameArea(mediaStorage, requiredMaxHeight = 535.dp)
                        PlaycontrolRow(nav, true, movieEntry, watched, sm) { showMediaInfoDialog = !showMediaInfoDialog }
                        HorizontalDivider(Modifier.fillMaxWidth().padding(10.dp))
                        MovieAboutView(mediaStorage, nav, sizes)
                    }
                }
            }
        }
    }

    @Composable
    fun MovieAboutView(mediaStorage: MediaStorage, nav: Navigator, sizes: LayoutSizes) {
        AboutView(mediaStorage.media, sizes)
        if (mediaStorage.hasSequel() || mediaStorage.hasPrequel()) {
            HorizontalDivider(Modifier.fillMaxWidth().padding(10.dp, 4.dp, 10.dp, 5.dp), thickness = 2.dp)
            MediaRelations(mediaStorage) {
                settings["episode-list-index"] = 0;
                nav.pushMediaView(it, true)
            }
        }
    }

    @Composable
    private fun PlaycontrolRow(
        nav: Navigator,
        limitWidth: Boolean = true,
        movieEntry: MediaEntry?,
        watched: MediaWatched?,
        mainVm: MainDataViewModel,
        onClickMediaInfo: () -> Unit
    ) {
        val mod = if (limitWidth) Modifier.padding(6.dp).widthIn(0.dp, 560.dp)
        else Modifier.padding(6.dp)
        Column(mod.fillMaxWidth()) {
            Row(Modifier.padding(3.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({
                    if (movieEntry == null)
                        return@IconButton
                    nav.push(PlayerView(movieEntry.GUID, watched.getProgress()))
                }) { Icon(Icons.Filled.PlayArrow, "Play this movie") }
                IconButton({
                    onClickMediaInfo()
                }) { Icon(Icons.Filled.Info, "Media information") }

                if (watched != null) {
                    IconButton(onClick = {
                        movieEntry?.let {
                            RequestQueue.removeWatched(movieEntry)
                            mainVm.updateData(true).joinAndSyncProgress(movieEntry, mainVm)
                        }
                    }) { Icon(Icons.Default.VisibilityOff, "Set Unwatched") }
                } else {
                    IconButton(onClick = {
                        movieEntry?.let {
                            RequestQueue.updateWatched(
                                MediaWatched(movieEntry.GUID, login?.userID ?: "", currentUnixSeconds(), 0, 0F, 100F)
                            )
                            mainVm.updateData(true).joinAndSyncProgress(movieEntry, mainVm)
                        }
                    }) { Icon(Icons.Default.Visibility, "Set Watched") }
                }
                if (movieEntry != null)
                    DownloadRow(movieEntry)
                Spacer(Modifier.weight(1f))
                Text(movieEntry?.fileSize?.readableSize() ?: "", style = MaterialTheme.typography.bodyMedium)
            }
            if (watched != null) {
                WatchedIndicator(watched, Modifier.fillMaxWidth().padding(0.dp, 2.dp, 0.dp, 5.dp))
            }
        }
    }

    @Composable
    private fun DownloadRow(entry: MediaEntry) {
        val downloaded by Storage.stores.downloadedStore.collectWithEmptyInitial()
        val currentlyDownloading by DownloadQueue.currentDownload.collectAsState()
        val queued by DownloadQueue.queuedEntries.collectAsState()
        Row(Modifier.padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            val isDownloaded = downloaded.find { it.entryID eqI entry.GUID } != null
            val isQueued = queued.contains(entry.GUID)
            val progress = currentlyDownloading?.let { if (it.entryID eqI entry.GUID) it else null }
            if (isQueued || progress != null) {
                if (isQueued) {
                    Icon(Icons.Default.Downloading, "Queued", modifier = Modifier.size(20.dp))
                } else if (progress != null) {
                    Box {
                        Icon(Icons.Default.Download, "Downloading", modifier = Modifier.size(14.dp).zIndex(1F).align(Alignment.Center))
                        CircularProgressIndicator(
                            { progress.progressPercent.toFloat() / 100 },
                            modifier = Modifier.size(23.dp).zIndex(2F).align(Alignment.Center),
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(0.4F),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
            if (progress == null) {
                if (isDownloaded || isQueued) {
                    IconButtonWithTooltip(Icons.Default.Delete, "Delete") {
                        if (queued.contains(entry.GUID)) {
                            launchThreaded { DownloadQueue.queuedEntries.emit(queued.toMutableList().filterNot { it eqI entry.GUID }.toList()) }
                        }
                        val downloadedEntry = downloaded.find { it.entryID eqI entry.GUID }
                        if (downloadedEntry != null) {
                            SYSTEMFILES.delete(downloadedEntry.okioPath)
                        }
                        launchThreaded {
                            Storage.stores.downloadedStore.updateList { list ->
                                list.removeAll { it.entryID eqI entry.GUID }
                            }
                        }
                    }
                } else {
                    IconButtonWithTooltip(Icons.Default.DownloadForOffline, "Download") {
                        DownloadQueue.addToQueue(entry)
                    }
                }
            }
        }
    }
}