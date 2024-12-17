package moe.styx.styx2m.views.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.russhwolf.settings.get
import kotlinx.coroutines.runBlocking
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.extensions.createTabOptions
import moe.styx.common.compose.extensions.getDistinctCategories
import moe.styx.common.compose.extensions.getDistinctGenres
import moe.styx.common.compose.files.Stores
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.SearchState
import moe.styx.common.compose.viewmodels.ListPosViewModel
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.common.compose.viewmodels.MainDataViewModelStorage
import moe.styx.common.data.Media
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.misc.LocalLayoutSize

class MediaTab(private val movies: Boolean = false, private val favourites: Boolean = false) : Tab {
    override val options: TabOptions
        @Composable
        get() {
            return if (favourites)
                createTabOptions("Favourites", Icons.Default.Star)
            else if (movies)
                createTabOptions("Movies", Icons.Default.Movie)
            else
                createTabOptions("Shows", Icons.Default.Tv)
        }

    override val key: ScreenKey
        get() {
            return if (favourites)
                "favourites-view"
            else if (movies)
                "movies-view"
            else
                "shows-view"
        }

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        val storage by sm.storageFlow.collectAsState()
        val searchStore = if (favourites) Stores.favSearchState else if (movies) Stores.movieSearchState else Stores.showSearchState

        val key = if (favourites) "favourites" else if (movies) "movies" else "shows"
        val useList = if (favourites) false else settings["$key-list", false]
        val listPosModel = nav.rememberNavigatorScreenModel("$key-pos-$useList") { ListPosViewModel() }

        val filtered = remember(storage) { storage.mediaList.filterMediaList(storage) }
        val availableGenres = remember(storage) { filtered.getDistinctGenres() }
        val availableCategories = remember(storage) { filtered.getDistinctCategories(storage.categoryList) }
        val initialState = runBlocking { searchStore.get() ?: SearchState() }
        val mediaSearch = remember(storage) { MediaSearch(searchStore, initialState, availableGenres, availableCategories, favourites) }
        Column {
            val sizes = LocalLayoutSize.current
            if (sizes.isWide) {
                val showFilters by mediaSearch.showFilters
                AnimatedVisibility(showFilters) {
                    mediaSearch.GenreCategoryFilters()
                }
            }
            MediaListing(
                mediaSearch,
                initialState,
                storage,
                filtered,
                useList,
                listPosModel,
                favourites,
                if (favourites) storage.favouritesList else emptyList(),
                showSearch = !sizes.isWide
            )
        }
    }

    @Composable
    fun HeaderSearchBar(modifier: Modifier = Modifier) {
        val nav = LocalGlobalNavigator.current
        val sizes = LocalLayoutSize.current

        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        val storage by sm.storageFlow.collectAsState()

        val searchStore = if (favourites) Stores.favSearchState else if (movies) Stores.movieSearchState else Stores.showSearchState
        val filtered = remember(storage) { storage.mediaList.filterMediaList(storage) }
        val availableGenres = remember(storage) { filtered.getDistinctGenres() }
        val availableCategories = remember(storage) { filtered.getDistinctCategories(storage.categoryList) }
        val initialState = runBlocking { searchStore.get() ?: SearchState() }
        val mediaSearch = remember(storage) { MediaSearch(searchStore, initialState, availableGenres, availableCategories, favourites) }
        if (sizes.isWide) {
            mediaSearch.Component(modifier.padding(10.dp), false)
        }
    }

    private fun List<Media>.filterMediaList(storage: MainDataViewModelStorage): List<Media> {
        return if (favourites)
            this.filter { m -> storage.favouritesList.find { it.mediaID eqI m.GUID } != null }
        else
            if (movies) this.filter { !it.isSeries.toBoolean() } else this.filter { it.isSeries.toBoolean() }
    }
}