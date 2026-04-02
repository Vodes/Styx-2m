package moe.styx.styx2m.views.tv.comp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import com.russhwolf.settings.get
import io.kamel.image.KamelImage
import kotlinx.coroutines.delay
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.anime.StupidImageNameArea
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.extensions.getPainter
import moe.styx.common.compose.extensions.removeSomeHTMLTags
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.viewmodels.ListPosViewModel
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.common.compose.viewmodels.MainDataViewModelStorage
import moe.styx.common.compose.viewmodels.MediaStorage
import moe.styx.common.data.Media
import moe.styx.common.extension.eqI
import moe.styx.common.util.Log
import moe.styx.styx2m.misc.handleDPadKeyEvents
import kotlin.math.max
import kotlin.math.min

@Composable
fun TvMediaBrowser(
    mediaSearch: MediaSearch,
    storage: MainDataViewModelStorage,
    mediaList: List<Media>,
    listPosViewModel: ListPosViewModel,
    showUnseen: Boolean = false
) {
    val nav = LocalGlobalNavigator.current
    val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
    var focusedIndex by remember { mutableStateOf<Int?>(listPosViewModel.scrollIndex) }
    val focusedMedia = focusedIndex?.let { mediaList.getOrNull(it) }
    val mediaStorage = focusedMedia?.let { sm.getMediaStorageForID(focusedMedia.GUID, storage) }
    val focusRequesters = remember { List(mediaList.size) { FocusRequester() } }

    LaunchedEffect(Unit) {
        if (mediaList.isNotEmpty()) {
            val initialIndex = (listPosViewModel.scrollIndex).coerceIn(0, mediaList.size - 1)
            delay(1000) // Small delay to ensure composition is complete
            focusRequesters.getOrNull(initialIndex)?.requestFocus()
        }
    }
    Column(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        AnimatedVisibility(mediaStorage != null) {
            if (mediaStorage == null)
                return@AnimatedVisibility
            CompositionLocalProvider(LocalDensity provides Density(density.density * 0.7F)) {
                TvMediaDetails(mediaStorage)
            }
        }
        Spacer(Modifier.weight(1f))
        MediaCarousel(storage, mediaList, listPosViewModel, focusedIndex = focusedIndex, focusRequesters = focusRequesters) {
            focusedIndex = min(mediaList.size, max(it, 0))
        }
        mediaSearch.Component(Modifier.fillMaxWidth().padding(10.dp))
    }
}

@Composable
fun TvMediaDetails(mediaStorage: MediaStorage) {
    Column {
        StupidImageNameArea(
            mediaStorage,
            requiredMaxHeight = 290.dp,
            requiredMaxWidth = 200.dp,
            mappingIconModifier = Modifier.padding(8.dp, 5.dp, 8.dp, 12.dp).size(30.dp),
            enforceConstraints = true,
            showMappingIcons = false,
        ) {
            Text("About", Modifier.padding(6.dp, 2.dp), style = MaterialTheme.typography.titleLarge)
            val preferGerman = settings["prefer-german-metadata", false]
            if (!mediaStorage.media.genres.isNullOrBlank()) {
                Text(
                    mediaStorage.media.genres!!,
                    Modifier.padding(6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val synopsis =
                if (!mediaStorage.media.synopsisDE.isNullOrBlank() && preferGerman) mediaStorage.media.synopsisDE else mediaStorage.media.synopsisEN
            if (!synopsis.isNullOrBlank())
                Text(
                    synopsis.removeSomeHTMLTags(),
                    Modifier.padding(6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
        }
    }
}

@Composable
fun MediaCarousel(
    storage: MainDataViewModelStorage,
    mediaList: List<Media>,
    listPosViewModel: ListPosViewModel,
    showUnseen: Boolean = false,
    focusedIndex: Int? = null,
    focusRequesters: List<FocusRequester> = emptyList(),
    onFocusChanged: (Int) -> Unit
) {
    val listState = rememberLazyListState(listPosViewModel.scrollIndex, listPosViewModel.scrollOffset)
    LaunchedEffect(focusedIndex) {
        focusedIndex?.let { index ->
            if (index in 0 until mediaList.size) {
                focusRequesters.getOrNull(index)?.requestFocus()
                if (listState.firstVisibleItemIndex != index) {
                    listState.animateScrollToItem(max(index - 2, 0))
                }
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            listPosViewModel.scrollIndex = listState.firstVisibleItemIndex
            listPosViewModel.scrollOffset = listState.firstVisibleItemScrollOffset
        }
    }
    LazyRow(Modifier.fillMaxWidth().focusable().handleDPadKeyEvents(onRight = {
        focusedIndex?.let { onFocusChanged(it + 1) } ?: onFocusChanged(1)
    }, onLeft = {
        focusedIndex?.let { onFocusChanged(it - 1) } ?: onFocusChanged(0)
    }, onEnter = {
        Log.d { "Open test: $focusedIndex" }
    }), state = listState) {
        items(mediaList, key = { it.GUID }) { media ->
            val image = storage.imageList.find { it.GUID eqI media.thumbID }
            if (image == null)
                return@items
            val index = mediaList.indexOf(media)
            val isFocused = focusedIndex == index
            Box(
                Modifier.animateItem().padding(4.dp).size(115.dp, 167.dp)
                    .focusRequester(focusRequesters.getOrNull(index) ?: FocusRequester())
                    .onFocusChanged {
                        if (it.isFocused)
                            onFocusChanged(index)
                    }.focusable()
                    .border(3.dp, if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent, AppShapes.medium)
            ) {
                val painter = image.getPainter()
                KamelImage(
                    { painter },
                    contentDescription = media.name,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.padding(2.dp).align(Alignment.Center)
                        .clip(AppShapes.medium),
                    animationSpec = tween(),
                    onLoading = { CircularProgressIndicator(progress = { it }) }
                )
            }
        }
    }
}