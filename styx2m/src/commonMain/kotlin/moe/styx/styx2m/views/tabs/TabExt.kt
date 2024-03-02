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
import com.russhwolf.settings.get
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import moe.styx.common.compose.components.anime.AnimeCard
import moe.styx.common.compose.components.anime.AnimeListItem
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.SearchState
import moe.styx.common.data.Media
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.misc.LayoutSizes
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.views.anime.AnimeDetailView
import moe.styx.styx2m.views.anime.MovieDetailView

object Tabs {
    val seriesTab = SeriesTab()
    val moviesTab by lazy { MoviesTab() }
    val favsTab = FavsTab()
    val scheduleTab by lazy { ScheduleTab() }
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
    val sizes = LocalLayoutSize.current
    Column(Modifier.fillMaxSize()) {
        mediaSearch.Component(Modifier.fillMaxWidth().padding(10.dp))
        Column(Modifier.fillMaxSize()) {
            val flow by mediaSearch.stateEmitter.debounce(150L).collectAsState(initialState)
            val processedMedia = flow.filterMedia(filtered)
            if (!useList)
                MediaGrid(processedMedia, nav, showUnseen, sizes)
            else
                MediaList(processedMedia, nav)
        }
    }
}

private fun getGridCells(sizes: LayoutSizes): GridCells {
    return if (sizes.isLandScape) {
        when (val value = settings["landscape-cards", "7"]) {
            "Adaptive" -> GridCells.Adaptive(sizes.height.dp / 3.3F)
            else -> {
                val number = value.toIntOrNull() ?: 7
                GridCells.Fixed(number)
            }
        }
    } else
        when (settings["portrait-cards", "3"]) {
            "3" -> GridCells.Fixed(3)
            "4" -> GridCells.Fixed(4)
            else -> GridCells.Fixed(5)
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGrid(list: List<Media>, nav: Navigator, showUnseen: Boolean = false, sizes: LayoutSizes) {
    LazyVerticalGrid(
        columns = getGridCells(sizes),
        contentPadding = PaddingValues(10.dp, 7.dp),
    ) {
        items(list, key = { it.GUID }) {
            Row(modifier = Modifier.animateItemPlacement()) {
                AnimeCard(it, showUnseen) {
                    if (it.isSeries.toBoolean()) {
                        nav.push(AnimeDetailView(it.GUID))
                    } else
                        nav.push(MovieDetailView(it.GUID))
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
                    if (it.isSeries.toBoolean()) {
                        nav.push(AnimeDetailView(it.GUID))
                    } else
                        nav.push(MovieDetailView(it.GUID))
                }
            }
        }
    }
}