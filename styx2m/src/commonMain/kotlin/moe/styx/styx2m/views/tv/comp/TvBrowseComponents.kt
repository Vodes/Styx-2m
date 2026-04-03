package moe.styx.styx2m.views.tv.comp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
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
import moe.styx.common.compose.components.anime.MediaGenreListing
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
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.misc.handleDPadKeyEvents
import moe.styx.styx2m.misc.pushMediaView
import kotlin.math.max

@Composable
fun TvMediaBrowser(
    mediaSearch: MediaSearch,
    storage: MainDataViewModelStorage,
    mediaList: List<Media>,
    listPosViewModel: ListPosViewModel,
    showUnseen: Boolean = false
) {
    val nav = LocalGlobalNavigator.current
    val listState = rememberLazyListState(listPosViewModel.scrollIndex, listPosViewModel.scrollOffset)
    val scope = rememberCoroutineScope()
    var focusedIndex by remember(mediaList.size) { mutableStateOf<Int?>(null) }
    val searchFocusRequester = remember { FocusRequester() }
    val imageById = remember(storage.updated, storage.imageList.size) {
        storage.imageList.associateBy { it.GUID.lowercase() }
    }
    val entriesByMediaId = remember(storage.updated, storage.entryList.size) {
        storage.entryList.groupBy { it.mediaID.lowercase() }
    }
    val watchedByEntryId = remember(storage.updated, storage.watchedList.size) {
        storage.watchedList.associateBy { it.entryID.lowercase() }
    }
    val focusRequesters = remember(mediaList.map { it.GUID }) {
        List(mediaList.size) { FocusRequester() }
    }

    suspend fun ensureItemVisible(index: Int) {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) {
            listState.scrollToItem(index)
            return
        }

        val firstVisible = visibleItems.first().index
        val lastVisible = visibleItems.last().index
        if (index < firstVisible) {
            listState.scrollToItem(index)
        } else if (index >= lastVisible) {
            listState.scrollToItem((index - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(mediaList.map { it.GUID }) {
        if (mediaList.isEmpty()) {
            focusedIndex = null
            return@LaunchedEffect
        }

        val initialIndex = max(0, listPosViewModel.scrollIndex.coerceAtMost(mediaList.lastIndex))
        ensureItemVisible(initialIndex)
        delay(32)
        focusRequesters.getOrNull(initialIndex)?.requestFocus()
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

    val preferredFocusIndex = remember(mediaList, focusedIndex, listPosViewModel.scrollIndex) {
        when {
            mediaList.isEmpty() -> null
            focusedIndex != null && focusedIndex in mediaList.indices -> focusedIndex
            else -> listPosViewModel.scrollIndex.coerceIn(0, mediaList.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        mediaSearch.Component(
            Modifier.fillMaxWidth()
                .padding(12.dp, 8.dp, 12.dp, 12.dp)
                .focusRequester(searchFocusRequester)
                .handleDPadKeyEvents(
                    onDown = {
                        preferredFocusIndex?.let { target ->
                            scope.launch {
                                ensureItemVisible(target)
                                focusRequesters.getOrNull(target)?.requestFocus()
                            }
                        }
                    }
                )
        )

        if (mediaList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No media matched the current filters.", style = MaterialTheme.typography.titleMedium)
            }
            return@Column
        }

        Spacer(Modifier.height(2.dp))

        ElevatedCard(
            Modifier.fillMaxSize().padding(10.dp, 0.dp, 10.dp, 10.dp),
            colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(mediaList, key = { _, media -> media.GUID }) { index, media ->
                    val mediaId = media.GUID.lowercase()
                    val entries = entriesByMediaId[mediaId].orEmpty()
                    val unseenCount = remember(showUnseen, entries, watchedByEntryId) {
                        if (!showUnseen) {
                            0
                        } else {
                            entries.count { (watchedByEntryId[it.GUID.lowercase()]?.maxProgress ?: 0F) < 85F }
                        }
                    }
                    TvMediaListItem(
                        media = media,
                        image = imageById[media.thumbID?.lowercase()],
                        entries = entries,
                        isFocused = focusedIndex == index,
                        focusRequester = focusRequesters[index],
                        unseenCount = unseenCount,
                        onFocused = {
                            focusedIndex = index
                            listPosViewModel.scrollIndex = index
                            listPosViewModel.scrollOffset = 0
                        },
                        onMoveUp = if (index > 0) {
                            { moveFocus(index, -1) }
                        } else {
                            {
                                scope.launch {
                                    searchFocusRequester.requestFocus()
                                }
                            }
                        },
                        onMoveDown = if (index < mediaList.lastIndex) {
                            { moveFocus(index, 1) }
                        } else {
                            null
                        },
                        onOpen = { nav.pushMediaView(media) }
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
    isFocused: Boolean,
    focusRequester: FocusRequester,
    unseenCount: Int,
    onFocused: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onOpen: () -> Unit
) {
    val preferGerman = settings["prefer-german-metadata", false]
    val synopsis = remember(media, preferGerman) {
        if (!media.synopsisDE.isNullOrBlank() && preferGerman) {
            media.synopsisDE
        } else {
            media.synopsisEN
        }?.removeSomeHTMLTags()
    }
    val painter = image?.getPainter()
    val scale by animateFloatAsState(if (isFocused) 1.015f else 1f, label = "tv-media-row-scale")
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
            .scale(scale)
            .focusRequester(focusRequester)
            .onFocusChanged {
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
                    3.dp,
                    if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    AppShapes.large
                ),
            shape = AppShapes.large,
            shadowElevation = if (isFocused) 14.dp else 3.dp,
            tonalElevation = if (isFocused) 10.dp else 1.dp,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(if (isFocused) 7.dp else 1.dp)
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
                            onLoading = { CircularProgressIndicator(progress = { it }, modifier = Modifier.padding(16.dp)) }
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
                        fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium
                    )
                    MediaGenreListing(media)
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
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        AnimatedVisibility(unseenCount > 0, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            Surface(
                shape = AppShapes.large,
                color = MaterialTheme.colorScheme.secondary,
                tonalElevation = 6.dp
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
