package moe.styx.styx2m.views.anime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import com.russhwolf.settings.set
import moe.styx.common.compose.components.anime.MediaInfoDialog
import moe.styx.common.compose.components.anime.MediaRelations
import moe.styx.common.compose.components.anime.WatchedIndicator
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.extensions.readableSize
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.collectWithEmptyInitial
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.http.login
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.RequestQueue
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaWatched
import moe.styx.common.extension.currentUnixSeconds
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.components.AboutView
import moe.styx.styx2m.components.StupidImageNameArea
import moe.styx.styx2m.misc.LayoutSizes
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.misc.getProgress
import moe.styx.styx2m.player.PlayerView

class MovieDetailView(private val mediaID: String) : Screen {

    override val key: ScreenKey
        get() = mediaID

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val mediaList by Storage.stores.mediaStore.getCurrentAndCollectFlow()
        val media = remember { mediaList.find { it.GUID eqI mediaID } }
        val movieEntry = fetchEntries(mediaID).minByOrNull { it.entryNumber.toDoubleOrNull() ?: 0.0 }
        if (media == null) {
            nav.pop()
            return
        }
        val sizes = LocalLayoutSize.current
        val watchedList by Storage.stores.watchedStore.collectWithEmptyInitial()
        val watched = movieEntry?.let { watchedList.find { it.entryID eqI movieEntry.GUID } }
        var showMediaInfoDialog by remember { mutableStateOf(false) }
        if (showMediaInfoDialog && movieEntry != null) {
            MediaInfoDialog(movieEntry) { showMediaInfoDialog = false }
        }
        MainScaffold(Modifier.fillMaxSize(), title = media.name) {
            val scrollState = rememberScrollState()
            ElevatedCard(Modifier.fillMaxSize().padding(2.dp)) {
                if (sizes.isWide) {
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.weight(0.5F).verticalScroll(scrollState)) {
                            StupidImageNameArea(media)
                            PlaycontrolRow(nav, false, movieEntry, watched) { showMediaInfoDialog = !showMediaInfoDialog }
                        }
                        VerticalDivider(Modifier.fillMaxHeight().padding(10.dp), thickness = 3.dp)
                        Column(Modifier.weight(0.5F).padding(0.dp, 8.dp).verticalScroll(rememberScrollState())) {
                            MovieAboutView(media, nav, mediaList, sizes)
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        StupidImageNameArea(media, otherContent = {
                            PlaycontrolRow(nav, true, movieEntry, watched) { showMediaInfoDialog = !showMediaInfoDialog }
                        })
                        HorizontalDivider(Modifier.fillMaxWidth().padding(10.dp))
                        MovieAboutView(media, nav, mediaList, sizes)
                    }
                }
            }
        }
    }

    @Composable
    fun MovieAboutView(media: Media, nav: Navigator, mediaList: List<Media>, sizes: LayoutSizes) {
        AboutView(media, sizes)
        if (!media.sequel.isNullOrBlank() || !media.prequel.isNullOrBlank()) {
            HorizontalDivider(Modifier.fillMaxWidth().padding(10.dp, 4.dp, 10.dp, 5.dp), thickness = 2.dp)
            MediaRelations(media, mediaList) {
                settings["episode-list-index"] = 0;
                if (it.isSeries.toBoolean())
                    nav.replace(AnimeDetailView(it.GUID))
                else
                    nav.replace(MovieDetailView(it.GUID))
            }
        }
    }

    @Composable
    private fun PlaycontrolRow(
        nav: Navigator,
        limitWidth: Boolean = true,
        movieEntry: MediaEntry?,
        watched: MediaWatched?,
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
                        movieEntry?.let { RequestQueue.removeWatched(movieEntry) }
                    }) { Icon(Icons.Default.VisibilityOff, "Set Unwatched") }
                } else {
                    IconButton(onClick = {
                        movieEntry?.let {
                            RequestQueue.updateWatched(
                                MediaWatched(movieEntry.GUID, login?.userID ?: "", currentUnixSeconds(), 0, 0F, 100F)
                            )
                        }
                    }) { Icon(Icons.Default.Visibility, "Set Watched") }
                }

                Spacer(Modifier.weight(1f))
                Text(movieEntry?.fileSize?.readableSize() ?: "", style = MaterialTheme.typography.bodyMedium)
            }
            if (watched != null) {
                WatchedIndicator(watched, Modifier.fillMaxWidth().padding(0.dp, 2.dp, 0.dp, 5.dp))
            }
        }
    }
}