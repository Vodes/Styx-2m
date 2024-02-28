package moe.styx.styx2m.views.tabs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import moe.styx.common.compose.components.anime.AnimeCard
import moe.styx.common.compose.components.anime.AnimeListItem
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.SearchState
import moe.styx.common.data.Media
import moe.styx.styx2m.views.anime.AnimeDetailView

object Tabs {
    val seriesTab = SeriesTab()
    val moviesTab by lazy { MoviesTab() }
    val favsTab = FavsTab()
}

@OptIn(FlowPreview::class)
@Composable
internal fun Tab.barWithListComp(
    mediaSearch: MediaSearch,
    initialState: SearchState,
    filtered: List<Media>,
    useList: Boolean = false,
    showUnseen: Boolean = false
) {
    val nav = LocalGlobalNavigator.current
    Column(Modifier.fillMaxSize()) {
        mediaSearch.Component(Modifier.fillMaxWidth().padding(10.dp))
        Column(Modifier.fillMaxSize()) {
            val flow by mediaSearch.stateEmitter.debounce(150L).collectAsState(initialState)
            val processedMedia = flow.filterMedia(filtered)
            if (!useList)
                MediaGrid(processedMedia, nav, showUnseen)
            else
                MediaList(processedMedia, nav)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGrid(list: List<Media>, nav: Navigator, showUnseen: Boolean = false) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(10.dp, 7.dp),
    ) {
        items(list, key = { it.GUID }) {
            Row(modifier = Modifier.animateItemPlacement()) {
                AnimeCard(it, showUnseen) {
                    nav.push(AnimeDetailView(it.GUID))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaList(list: List<Media>, nav: Navigator) {
    LazyColumn {
        items(list, key = { it.GUID }) {
            Row(Modifier.animateItemPlacement().padding(3.dp)) {
                AnimeListItem(it) {
                    nav.push(AnimeDetailView(it.GUID))
                }
            }
        }
    }
}