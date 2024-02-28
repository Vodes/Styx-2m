package moe.styx.styx2m.views.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import kotlinx.coroutines.runBlocking
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.extensions.createTabOptions
import moe.styx.common.compose.extensions.isFav
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.utils.SearchState

class FavsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            return createTabOptions("Favourites", Icons.Default.Star)
        }

    @Composable
    override fun Content() {
        val media by Storage.stores.mediaStore.getCurrentAndCollectFlow()
        val searchStore = Storage.stores.favSearchState
        val filtered = media.filter { it.isFav() }
        val initialState = runBlocking { searchStore.get() ?: SearchState() }
        val mediaSearch = MediaSearch(searchStore, initialState, emptyList(), emptyList(), true)
        barWithListComp(mediaSearch, initialState, filtered, false, true)
    }
}