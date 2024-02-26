package moe.styx.styx2m.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import moe.styx.common.compose.components.anime.AnimeCard
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.styx2m.views.anime.AnimeDetailView

class TestOverview : Screen {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val media by Storage.stores.mediaStore.getCurrentAndCollectFlow()
        MainScaffold(title = "Test", addPopButton = false) {
            Column(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(10.dp, 7.dp),
                ) {
                    itemsIndexed(
                        items = media, key = { _, item -> item.GUID },
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