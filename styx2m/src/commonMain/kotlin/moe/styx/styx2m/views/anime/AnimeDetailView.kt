package moe.styx.styx2m.views.anime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.moriatsushi.insetsx.SystemBarsBehavior
import com.moriatsushi.insetsx.rememberWindowInsetsController
import com.moriatsushi.insetsx.safeAreaPadding
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import moe.styx.common.compose.components.anime.EpisodeList
import moe.styx.common.compose.components.buttons.FavouriteIconButton
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.collectWithEmptyInitial
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.Heartbeats
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.eqI
import moe.styx.styx2m.components.MetadataArea
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.misc.getProgress
import moe.styx.styx2m.player.PlayerView

class AnimeDetailView(private val mediaID: String) : Screen {

    override val key: ScreenKey
        get() = mediaID

    @OptIn(FlowPreview::class)
    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val insets = rememberWindowInsetsController()
        insets?.setIsNavigationBarsVisible(true)
        insets?.setIsStatusBarsVisible(true)
        insets?.setSystemBarsBehavior(SystemBarsBehavior.Default)
        val mediaList by Storage.stores.mediaStore.getCurrentAndCollectFlow()
        val media = remember { mediaList.find { it.GUID eqI mediaID } }
        if (media == null) {
            nav.pop()
            return
        }
        val entries = fetchEntries(mediaID)
        val watched by Storage.stores.watchedStore.collectWithEmptyInitial()

        MainScaffold(Modifier.safeAreaPadding(), title = media.name, actions = {
            FavouriteIconButton(media)
        }) {
            val showSelection = remember { mutableStateOf(false) }
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = settings["episode-list-index", 0])
            LaunchedEffect(listState) {
                snapshotFlow { listState.firstVisibleItemIndex }
                    .debounce(400L).collectLatest { settings["episode-list-index"] = it }
            }
            val sizes = LocalLayoutSize.current
            ElevatedCard(Modifier.padding(2.dp).fillMaxSize()) {
                if (!sizes.isWide) {
                    Column {
                        EpisodeList(
                            entries,
                            showSelection,
                            null,
                            listState,
                            onPlay = {
                                nav.push(PlayerView(it.GUID, watched.find { w -> w.entryID eqI it.GUID }.getProgress()))
                                ""
                            }) {
                            MetadataArea(media, nav, mediaList, layoutSizes = sizes)
                            HorizontalDivider(Modifier.fillMaxWidth().padding(10.dp, 8.dp), thickness = 3.dp)
                        }
                    }
                } else {
                    val scrollState = rememberScrollState()
                    Row {
                        Column(Modifier.weight(0.5F).verticalScroll(scrollState)) {
                            MetadataArea(media, nav, mediaList, Modifier.fillMaxHeight(0.6F).heightIn(0.dp, 500.dp), layoutSizes = sizes)
                        }
                        VerticalDivider(Modifier.padding(2.dp, 8.dp).fillMaxHeight().width(3.dp))
                        Column(Modifier.weight(0.5F)) {
                            EpisodeList(entries, showSelection, null, listState, onPlay = {
                                nav.push(PlayerView(it.GUID, watched.find { w -> w.entryID eqI it.GUID }.getProgress()))
                                ""
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun fetchEntries(mediaID: String): List<MediaEntry> {
    Heartbeats.mediaActivity = null
    val flow by Storage.stores.entryStore.getCurrentAndCollectFlow()
    val filtered = flow.filter { it.mediaID eqI mediaID }
    return if (settings["episode-asc", false]) filtered.sortedBy {
        it.entryNumber.toDoubleOrNull() ?: 0.0
    } else filtered.sortedByDescending { it.entryNumber.toDoubleOrNull() ?: 0.0 }
}