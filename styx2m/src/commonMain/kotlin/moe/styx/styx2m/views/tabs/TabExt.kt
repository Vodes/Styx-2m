package moe.styx.styx2m.views.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import moe.styx.common.compose.viewmodels.ListPosViewModel
import moe.styx.common.compose.viewmodels.MainDataViewModelStorage
import moe.styx.common.data.Favourite
import moe.styx.common.data.Media
import moe.styx.common.extension.eqI
import moe.styx.styx2m.misc.LayoutSizes
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.misc.pushMediaView

object Tabs {
    val seriesTab = MediaTab()
    val moviesTab by lazy { MediaTab(movies = true) }
    val favsTab = MediaTab(favourites = true)
    val scheduleTab by lazy { ScheduleTab() }
}

@OptIn(FlowPreview::class)
@Composable
internal fun Tab.barWithListComp(
    mediaSearch: MediaSearch,
    initialState: SearchState,
    storage: MainDataViewModelStorage,
    filtered: List<Media>,
    useList: Boolean = false,
    listPosViewModel: ListPosViewModel,
    showUnseen: Boolean = false,
    favourites: List<Favourite> = emptyList()
) {
    val sizes = LocalLayoutSize.current
    Column(Modifier.fillMaxSize()) {
        mediaSearch.Component(Modifier.fillMaxWidth().padding(10.dp))
        Column(Modifier.fillMaxSize()) {
            val flow by mediaSearch.stateEmitter.debounce(150L).collectAsState(initialState)
            val processedMedia = flow.filterMedia(filtered, favourites)
            if (!useList)
                MediaGrid(storage, processedMedia, listPosViewModel, showUnseen, sizes)
            else
                MediaList(storage, processedMedia, listPosViewModel)
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

@Composable
private fun MediaGrid(
    storage: MainDataViewModelStorage,
    mediaList: List<Media>,
    listPosViewModel: ListPosViewModel,
    showUnseen: Boolean = false,
    sizes: LayoutSizes
) {
    val nav = LocalGlobalNavigator.current
    val listState = rememberLazyGridState(listPosViewModel.scrollIndex, listPosViewModel.scrollOffset)
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            listPosViewModel.scrollIndex = listState.firstVisibleItemIndex
            listPosViewModel.scrollOffset = listState.firstVisibleItemScrollOffset
        }
    }
    if (showUnseen) {
        LazyVerticalGrid(
            columns = getGridCells(sizes),
            contentPadding = PaddingValues(10.dp, 7.dp),
            state = listState
        ) {
            items(mediaList, key = { it.GUID }) {
                Row(modifier = Modifier.animateItem()) {
                    AnimeCard(
                        it to storage.imageList.find { img -> img.GUID eqI it.thumbID },
                        true,
                        entryList = storage.entryList,
                        watchedEntries = storage.watchedList
                    ) { nav.pushMediaView(it) }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = getGridCells(sizes),
            contentPadding = PaddingValues(10.dp, 7.dp),
            state = listState
        ) {
            items(mediaList, key = { it.GUID }) {
                Row(modifier = Modifier.animateItem()) {
                    AnimeCard(it to storage.imageList.find { img -> img.GUID eqI it.thumbID }) { nav.pushMediaView(it) }
                }
            }
        }
    }
}

@Composable
private fun MediaList(storage: MainDataViewModelStorage, mediaList: List<Media>, listPosViewModel: ListPosViewModel) {
    val nav = LocalGlobalNavigator.current
    val listState = rememberLazyListState(listPosViewModel.scrollIndex, listPosViewModel.scrollOffset)
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            listPosViewModel.scrollIndex = listState.firstVisibleItemIndex
            listPosViewModel.scrollOffset = listState.firstVisibleItemScrollOffset
        }
    }
    LazyColumn(state = listState) {
        items(mediaList, key = { it.GUID }) {
            Row(Modifier.animateItem().padding(3.dp)) {
                AnimeListItem(it, storage.imageList.find { img -> img.GUID eqI it.thumbID }) { nav.pushMediaView(it) }
            }
        }
    }
}