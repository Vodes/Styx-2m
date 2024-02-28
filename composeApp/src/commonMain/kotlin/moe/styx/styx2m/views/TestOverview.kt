package moe.styx.styx2m.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.runBlocking
import moe.styx.common.compose.components.anime.AnimeCard
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.SearchState
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.views.anime.AnimeDetailView

class TestOverview : Screen {

    @OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val media by Storage.stores.mediaStore.getCurrentAndCollectFlow()
        val categories by Storage.stores.categoryStore.getCurrentAndCollectFlow()
        val searchStore = Storage.stores.showSearchState
        val filtered = media.filter { it.isSeries.toBoolean() }
        val availableGenres = filtered.flatMap { m ->
            (m.genres ?: "").split(",").map { it.trim() }
        }.distinct().filter { it.isNotBlank() }.sorted().toList()
        val availableCategories =
            filtered.asSequence().map { (it.categoryID ?: "").trim() }
                .distinct().filter { it.isNotBlank() }
                .mapNotNull { categories.find { cat -> cat.GUID eqI it } }
                .filter { it.isVisible.toBoolean() }.sortedBy { it.sort }.toList()
        val initialState = runBlocking { searchStore.get() ?: SearchState() }
        val mediaSearch = MediaSearch(searchStore, initialState, availableGenres, availableCategories)
        MainScaffold(title = "Test", addPopButton = false) {
            Column(Modifier.fillMaxSize()) {
                mediaSearch.Component(Modifier.fillMaxWidth().padding(10.dp))
                Column(Modifier.fillMaxSize()) {
                    val flow by mediaSearch.stateEmitter.debounce(150L).collectAsState(initialState)
                    val processedMedia = flow.filterMedia(media)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(10.dp, 7.dp),
                    ) {
                        itemsIndexed(
                            items = processedMedia, key = { _, item -> item.GUID },
                        ) { _, item ->
                            Row(modifier = Modifier.animateItemPlacement()) {
                                AnimeCard(item, false) {
                                    nav.push(AnimeDetailView(item.GUID))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}