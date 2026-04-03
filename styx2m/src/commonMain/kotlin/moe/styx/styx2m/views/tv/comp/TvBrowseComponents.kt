package moe.styx.styx2m.views.tv.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.get
import io.kamel.image.KamelImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.extensions.clickableNoIndicator
import moe.styx.common.compose.extensions.getPainter
import moe.styx.common.compose.extensions.readableSize
import moe.styx.common.compose.extensions.removeSomeHTMLTags
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.viewmodels.ListPosViewModel
import moe.styx.common.compose.viewmodels.MainDataViewModelStorage
import moe.styx.common.data.Image
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.eqI
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.misc.handleDPadKeyEvents
import moe.styx.styx2m.misc.pushMediaView

@Composable
fun TvMediaBrowser(
    mediaSearch: MediaSearch,
    storage: MainDataViewModelStorage,
    mediaList: List<Media>,
    listPosViewModel: ListPosViewModel,
    showUnseen: Boolean = false
) {
    val nav = LocalGlobalNavigator.current
    val scope = rememberCoroutineScope()
    var focusedIndex by remember { mutableStateOf<Int?>(null) }
    var headerHasFocus by remember { mutableStateOf(false) }
    var initialListFocusHandled by remember { mutableStateOf(false) }
    var resetListTargetToStart by remember { mutableStateOf(false) }
    var previousMediaIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val searchFocusRequester = remember { FocusRequester() }
    val controlsFocusRequester = remember { FocusRequester() }
    val mediaIds = remember(mediaList) { mediaList.map(Media::GUID) }
    val initialListItemIndex = remember(listPosViewModel.scrollIndex, listPosViewModel.scrollOffset, listPosViewModel.focusedKey) {
        if (
            listPosViewModel.scrollIndex > 0 ||
            listPosViewModel.scrollOffset > 0 ||
            listPosViewModel.focusedKey.isNotBlank()
        ) {
            listPosViewModel.scrollIndex + 1
        } else {
            0
        }
    }
    val listState = rememberLazyListState(initialListItemIndex, listPosViewModel.scrollOffset)
    val imageById = remember(storage.updated, storage.imageList.size) {
        storage.imageList.associateBy { it.GUID.lowercase() }
    }
    val entriesByMediaId = remember(storage.updated, storage.entryList.size) {
        storage.entryList.groupBy { it.mediaID.lowercase() }
    }
    val watchedByEntryId = remember(storage.updated, storage.watchedList.size) {
        storage.watchedList.associateBy { it.entryID.lowercase() }
    }
    val unseenCountByMediaId = remember(showUnseen, entriesByMediaId, watchedByEntryId) {
        if (!showUnseen) {
            emptyMap()
        } else {
            entriesByMediaId.mapValues { (_, entries) ->
                entries.count { (watchedByEntryId[it.GUID.lowercase()]?.maxProgress ?: 0F) < 85F }
            }
        }
    }
    val focusRequesters = remember(mediaIds) {
        List(mediaList.size) { FocusRequester() }
    }

    suspend fun ensureItemVisible(index: Int) {
        val listIndex = index + 1
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) {
            listState.scrollToItem(listIndex)
            return
        }

        val firstVisible = visibleItems.first().index
        val lastVisible = visibleItems.last().index
        if (listIndex < firstVisible) {
            listState.scrollToItem(listIndex)
        } else if (listIndex >= lastVisible) {
            listState.scrollToItem((listIndex - 1).coerceAtLeast(0))
        }
    }

    val preferredFocusIndex = remember(mediaList, focusedIndex, listPosViewModel.scrollIndex, listPosViewModel.focusedKey, resetListTargetToStart) {
        when {
            mediaList.isEmpty() -> null
            resetListTargetToStart -> 0
            listPosViewModel.focusedKey.isNotBlank() -> {
                mediaList.indexOfFirst { it.GUID eqI listPosViewModel.focusedKey }
                    .takeIf { it >= 0 }
            }
            focusedIndex != null && focusedIndex in mediaList.indices -> focusedIndex
            else -> listPosViewModel.scrollIndex.coerceIn(0, mediaList.lastIndex)
        }
    }

    LaunchedEffect(mediaIds) {
        val resultsChanged = previousMediaIds != mediaIds
        previousMediaIds = mediaIds

        if (resultsChanged && headerHasFocus) {
            focusedIndex = null
            resetListTargetToStart = true
            listPosViewModel.scrollIndex = 0
            listPosViewModel.scrollOffset = 0
            listPosViewModel.focusedKey = ""
            listState.scrollToItem(0)
        }

        if (mediaList.isEmpty()) {
            focusedIndex = null
            return@LaunchedEffect
        }

        val initialIndex = preferredFocusIndex ?: 0
        if (!initialListFocusHandled && !headerHasFocus) {
            ensureItemVisible(initialIndex)
            delay(32)
            focusRequesters.getOrNull(initialIndex)?.requestFocus()
            initialListFocusHandled = true
        } else if (focusedIndex !in mediaList.indices) {
            focusedIndex = initialIndex
        }
    }

    fun moveFocus(fromIndex: Int, step: Int) {
        if (mediaList.isEmpty()) {
            return
        }

        val target = fromIndex + step
        if (target !in mediaList.indices) {
            return
        }

        scope.launch {
            ensureItemVisible(target)
            focusRequesters.getOrNull(target)?.requestFocus()
        }
    }

    fun focusListTarget() {
        preferredFocusIndex?.let { target ->
            scope.launch {
                ensureItemVisible(target)
                focusRequesters.getOrNull(target)?.requestFocus()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            Modifier.fillMaxSize().padding(10.dp, 8.dp, 10.dp, 10.dp),
            shape = AppShapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            tonalElevation = 1.dp
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "tv-search-header") {
                    TvMediaSearchHeader(
                        mediaSearch = mediaSearch,
                        searchFocusRequester = searchFocusRequester,
                        controlsFocusRequester = controlsFocusRequester,
                        onHeaderFocused = {
                            headerHasFocus = true
                        },
                        onMoveDownToList = {
                            if (mediaList.isNotEmpty()) {
                                focusListTarget()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mediaList.isEmpty()) {
                    item(key = "tv-empty-state") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No media matched the current filters.", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    return@LazyColumn
                }

                itemsIndexed(mediaList, key = { _, media -> media.GUID }) { index, media ->
                    val mediaId = media.GUID.lowercase()
                    val entries = entriesByMediaId[mediaId].orEmpty()
                    TvMediaListItem(
                        media = media,
                        image = imageById[media.thumbID?.lowercase()],
                        entries = entries,
                        focusRequester = focusRequesters[index],
                        unseenCount = unseenCountByMediaId[mediaId] ?: 0,
                        onFocused = {
                            headerHasFocus = false
                            resetListTargetToStart = false
                            focusedIndex = index
                            listPosViewModel.scrollIndex = index
                            listPosViewModel.scrollOffset = 0
                            listPosViewModel.focusedKey = media.GUID
                        },
                        onMoveUp = if (index > 0) {
                            { moveFocus(index, -1) }
                        } else {
                            {
                                scope.launch {
                                    controlsFocusRequester.requestFocus()
                                }
                            }
                        },
                        onMoveDown = if (index < mediaList.lastIndex) {
                            { moveFocus(index, 1) }
                        } else {
                            null
                        },
                        onOpen = {
                            listPosViewModel.focusedKey = media.GUID
                            nav.pushMediaView(media)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvMediaListItem(
    media: Media,
    image: Image?,
    entries: List<MediaEntry>,
    focusRequester: FocusRequester,
    unseenCount: Int,
    onFocused: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onOpen: () -> Unit
) {
    val preferGerman = settings["prefer-german-metadata", false]
    var hasFocus by remember { mutableStateOf(false) }
    val synopsis = remember(media, preferGerman) {
        if (!media.synopsisDE.isNullOrBlank() && preferGerman) {
            media.synopsisDE
        } else {
            media.synopsisEN
        }?.removeSomeHTMLTags()
    }
    val genresLabel = remember(media.genres) {
        media.genres?.split(",")
            ?.asSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.take(3)
            ?.joinToString("  •  ")
            .orEmpty()
    }
    val painter = image?.getPainter()
    val interactionSource = remember { MutableInteractionSource() }
    val progressLabel = remember(media, entries, unseenCount) {
        if (media.isSeries.toBoolean()) {
            buildList {
                add("${entries.size} episodes")
                if (unseenCount > 0) {
                    add("$unseenCount unwatched")
                }
            }.joinToString("  •  ")
        } else {
            entries.firstOrNull()?.fileSize?.readableSize() ?: "Movie"
        }
    }

    Box(
        Modifier.fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                hasFocus = it.isFocused
                if (it.isFocused) {
                    onFocused()
                }
            }
            .handleDPadKeyEvents(onUp = onMoveUp, onDown = onMoveDown, onEnter = onOpen)
            .focusable(interactionSource = interactionSource)
            .clickableNoIndicator { onOpen() }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .border(
                    2.dp,
                    if (hasFocus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    AppShapes.large
                ),
            shape = AppShapes.large,
            tonalElevation = if (hasFocus) 4.dp else 1.dp,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(if (hasFocus) 4.dp else 1.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(Modifier.width(96.dp).aspectRatio(0.71f)) {
                    if (painter != null) {
                        KamelImage(
                            { painter },
                            contentDescription = media.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().padding(2.dp).clip(AppShapes.medium),
                            onLoading = {
                                Box(
                                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                media.name,
                                modifier = Modifier.padding(12.dp),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        media.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = if (hasFocus) FontWeight.SemiBold else FontWeight.Medium
                    )
                    if (genresLabel.isNotBlank()) {
                        Text(
                            genresLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        progressLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!synopsis.isNullOrBlank()) {
                        Text(
                            synopsis,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Text(
                        "Press center to open details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasFocus) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        if (unseenCount > 0) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                shape = AppShapes.large,
                color = MaterialTheme.colorScheme.secondary,
                tonalElevation = 3.dp
            ) {
                Text(
                    unseenCount.toString(),
                    modifier = Modifier.padding(10.dp, 5.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
