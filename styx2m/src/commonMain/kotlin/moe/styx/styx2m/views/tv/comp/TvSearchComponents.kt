package moe.styx.styx2m.views.tv.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.search.MediaSearch
import moe.styx.common.compose.extensions.clickableNoIndicator
import moe.styx.common.compose.utils.SearchState
import moe.styx.common.compose.utils.SortType
import moe.styx.common.data.Category
import moe.styx.styx2m.misc.handleDPadKeyEvents

@Composable
internal fun TvMediaSearchHeader(
    mediaSearch: MediaSearch,
    searchFocusRequester: FocusRequester,
    controlsFocusRequester: FocusRequester,
    onHeaderFocused: () -> Unit,
    onMoveDownToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val state by mediaSearch.stateEmitter.collectAsState(mediaSearch.currentState)
    val activeFilterCount = state.selectedCategories.size + state.selectedGenres.size
    val hasResettableState = state.search.isNotBlank() || activeFilterCount > 0
    var showSortDialog by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.search,
            onValueChange = { mediaSearch.updateState { current -> current.copy(search = it) } },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        onHeaderFocused()
                    }
                }
                .handleDPadKeyEvents(
                    onDown = onMoveDownToList
                ),
            singleLine = true,
            label = { Text("Search") },
            shape = AppShapes.medium
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TvSearchActionButton(
                text = "Sort: ${state.sortType.displayName}",
                modifier = Modifier.weight(1f),
                focusRequester = controlsFocusRequester,
                onFocused = onHeaderFocused,
                onMoveUp = {
                    scope.launch { searchFocusRequester.requestFocus() }
                },
                onMoveDown = onMoveDownToList,
                onClick = { showSortDialog = true }
            )

            TvSearchActionButton(
                text = if (state.sortDescending) "Descending" else "Ascending",
                modifier = Modifier.weight(1f),
                onFocused = onHeaderFocused,
                onMoveUp = {
                    scope.launch { searchFocusRequester.requestFocus() }
                },
                onMoveDown = onMoveDownToList,
                onClick = {
                    mediaSearch.updateState { current ->
                        current.copy(sortDescending = !current.sortDescending)
                    }
                }
            )

            if (mediaSearch.supportsFilters) {
                TvSearchActionButton(
                    text = if (activeFilterCount > 0) "Filters ($activeFilterCount)" else "Filters",
                    modifier = Modifier.weight(1f),
                    onFocused = onHeaderFocused,
                    onMoveUp = {
                        scope.launch { searchFocusRequester.requestFocus() }
                    },
                    onMoveDown = onMoveDownToList,
                    onClick = { showFiltersDialog = true }
                )
            }

            if (hasResettableState) {
                TvSearchActionButton(
                    text = "Reset",
                    modifier = Modifier.weight(0.85f),
                    onFocused = onHeaderFocused,
                    onMoveUp = {
                        scope.launch { searchFocusRequester.requestFocus() }
                    },
                    onMoveDown = onMoveDownToList,
                    onClick = {
                        mediaSearch.updateState { current ->
                            current.copy(
                                search = "",
                                selectedGenres = emptyList(),
                                selectedCategories = emptyList()
                            )
                        }
                    }
                )
            }
        }
    }

    if (showSortDialog) {
        TvSortSelectionDialog(
            state = state,
            onDismiss = { showSortDialog = false },
            onSelect = { sortType ->
                mediaSearch.updateState { current -> current.copy(sortType = sortType) }
                showSortDialog = false
            }
        )
    }

    if (showFiltersDialog) {
        TvFilterSelectionDialog(
            state = state,
            availableCategories = mediaSearch.categories,
            availableGenres = mediaSearch.genres,
            onDismiss = { showFiltersDialog = false },
            onToggleCategory = { category ->
                mediaSearch.updateState { current ->
                    val selectedCategories = current.selectedCategories.toMutableList()
                    if (selectedCategories.any { it.GUID.equals(category.GUID, true) }) {
                        selectedCategories.removeAll { it.GUID.equals(category.GUID, true) }
                    } else {
                        selectedCategories.add(category)
                    }
                    current.copy(selectedCategories = selectedCategories)
                }
            },
            onToggleGenre = { genre ->
                mediaSearch.updateState { current ->
                    val selectedGenres = current.selectedGenres.toMutableList()
                    val existingIndex = selectedGenres.indexOfFirst { it.equals(genre, true) }
                    if (existingIndex >= 0) {
                        selectedGenres.removeAt(existingIndex)
                    } else {
                        selectedGenres.add(genre)
                    }
                    current.copy(selectedGenres = selectedGenres)
                }
            }
        )
    }
}

@Composable
private fun TvSortSelectionDialog(
    state: SearchState,
    onDismiss: () -> Unit,
    onSelect: (SortType) -> Unit
) {
    val focusRequesters = remember { List(SortType.entries.size) { FocusRequester() } }
    val initialIndex = remember(state.sortType) {
        SortType.entries.indexOf(state.sortType).coerceAtLeast(0)
    }

    LaunchedEffect(initialIndex) {
        delay(64)
        focusRequesters.getOrNull(initialIndex)?.requestFocus()
    }

    TvSelectionDialog(
        title = "Sort By",
        subtitle = "Pick how media is ordered in the list.",
        onDismiss = onDismiss
    ) {
        SortType.entries.forEachIndexed { index, sortType ->
            TvSelectionRow(
                text = sortType.displayName,
                selected = state.sortType == sortType,
                focusRequester = focusRequesters[index],
                onClick = { onSelect(sortType) }
            )
        }
    }
}

@Composable
private fun TvFilterSelectionDialog(
    state: SearchState,
    availableCategories: List<Category>,
    availableGenres: List<String>,
    onDismiss: () -> Unit,
    onToggleCategory: (Category) -> Unit,
    onToggleGenre: (String) -> Unit
) {
    val sortedCategories = remember(availableCategories) { availableCategories.sortedByDescending { it.sort } }
    val sortedGenres = remember(availableGenres) { availableGenres.sortedBy { it.lowercase() } }
    val totalOptionCount = sortedCategories.size + sortedGenres.size
    val focusRequesters = remember(totalOptionCount) { List(totalOptionCount) { FocusRequester() } }
    val selectedCategoryIds = remember(state.selectedCategories) { state.selectedCategories.map { it.GUID.lowercase() }.toSet() }
    val selectedGenres = remember(state.selectedGenres) { state.selectedGenres.map { it.lowercase() }.toSet() }
    val initialIndex = remember(sortedCategories, sortedGenres, selectedCategoryIds, selectedGenres) {
        val selectedCategoryIndex = sortedCategories.indexOfFirst { it.GUID.lowercase() in selectedCategoryIds }
        when {
            selectedCategoryIndex >= 0 -> selectedCategoryIndex
            else -> {
                val selectedGenreIndex = sortedGenres.indexOfFirst { it.lowercase() in selectedGenres }
                if (selectedGenreIndex >= 0) sortedCategories.size + selectedGenreIndex else 0
            }
        }
    }

    LaunchedEffect(totalOptionCount, initialIndex) {
        if (totalOptionCount <= 0) {
            return@LaunchedEffect
        }
        delay(64)
        focusRequesters.getOrNull(initialIndex)?.requestFocus()
    }

    TvSelectionDialog(
        title = "Filters",
        subtitle = "Toggle categories and genres. Press back when done.",
        onDismiss = onDismiss
    ) {
        if (sortedCategories.isNotEmpty()) {
            Text(
                "Categories",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            sortedCategories.forEachIndexed { index, category ->
                TvSelectionRow(
                    text = category.name,
                    selected = category.GUID.lowercase() in selectedCategoryIds,
                    focusRequester = focusRequesters[index],
                    onClick = { onToggleCategory(category) }
                )
            }
        }

        if (sortedGenres.isNotEmpty()) {
            if (sortedCategories.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
            Text(
                "Genres",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            sortedGenres.forEachIndexed { index, genre ->
                TvSelectionRow(
                    text = genre,
                    selected = genre.lowercase() in selectedGenres,
                    focusRequester = focusRequesters[sortedCategories.size + index],
                    onClick = { onToggleGenre(genre) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TvSelectionDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f))
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp && (it.key == Key.Back || it.key == Key.Escape)) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .heightIn(max = 760.dp),
                shape = AppShapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun TvSelectionRow(
    text: String,
    selected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .handleDPadKeyEvents(onEnter = onClick)
            .focusable(interactionSource = interactionSource)
            .clickableNoIndicator { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    if (isFocused) 3.dp else 1.dp,
                    when {
                        isFocused -> MaterialTheme.colorScheme.onSurface
                        selected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                    },
                    AppShapes.large
                ),
            shape = AppShapes.large,
            color = when {
                isFocused -> MaterialTheme.colorScheme.primaryContainer
                selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else -> MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            },
            tonalElevation = 0.dp
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected || isFocused) FontWeight.SemiBold else FontWeight.Medium
                )
                Text(
                    if (selected) "On" else "Off",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TvSearchActionButton(
    text: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onFocused()
                }
            }
            .handleDPadKeyEvents(
                onUp = onMoveUp,
                onDown = onMoveDown
            )
            .heightIn(min = 42.dp)
            .widthIn(min = 120.dp),
        shape = AppShapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            },
            contentColor = if (isFocused) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, focusedElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(
            text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
