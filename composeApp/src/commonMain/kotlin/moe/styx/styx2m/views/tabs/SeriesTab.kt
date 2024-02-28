package moe.styx.styx2m.views.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.russhwolf.settings.get
import kotlinx.coroutines.runBlocking
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.extensions.createTabOptions
import moe.styx.common.compose.extensions.getDistinctCategories
import moe.styx.common.compose.extensions.getDistinctGenres
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.SearchState
import moe.styx.common.extension.toBoolean

class SeriesTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            return createTabOptions("Shows", Icons.Default.Tv)
        }

    @Composable
    override fun Content() {
        val media by Storage.stores.mediaStore.getCurrentAndCollectFlow()
        val categories by Storage.stores.categoryStore.getCurrentAndCollectFlow()
        val searchStore = Storage.stores.showSearchState
        val filtered = media.filter { it.isSeries.toBoolean() }
        val availableGenres = filtered.getDistinctGenres()
        val availableCategories = filtered.getDistinctCategories(categories)
        val initialState = runBlocking { searchStore.get() ?: SearchState() }
        val mediaSearch = MediaSearch(searchStore, initialState, availableGenres, availableCategories)
        barWithListComp(mediaSearch, initialState, filtered, settings["shows-list", false])
    }
}