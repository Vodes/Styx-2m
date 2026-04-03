package moe.styx.styx2m.views.tv

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.russhwolf.settings.get
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.anime.EpisodeList
import moe.styx.common.compose.components.anime.MediaGenreListing
import moe.styx.common.compose.components.anime.MediaRelations
import moe.styx.common.compose.components.anime.StupidImageNameArea
import moe.styx.common.compose.components.buttons.FavouriteIconButton
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.extensions.removeSomeHTMLTags
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.RequestQueue
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.common.compose.viewmodels.MainDataViewModelStorage
import moe.styx.common.compose.viewmodels.MediaStorage
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.misc.pushMediaView
import moe.styx.styx2m.player.PlayerView
import moe.styx.styx2m.views.tv.comp.entryToPlay
import moe.styx.styx2m.views.tv.comp.playLabel
import moe.styx.styx2m.views.tv.comp.progressFor

class TvMediaDetailView(private val mediaID: String) : Screen {

    override val key: ScreenKey
        get() = mediaID

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        val storage by sm.storageFlow.collectAsState()
        val mediaStorage = remember(storage) { sm.getMediaStorageForID(mediaID, storage) }

        MainScaffold(
            title = mediaStorage.media.name,
            actions = {
                FavouriteIconButton(mediaStorage.media, sm, storage)
            }
        ) {
            if (mediaStorage.media.isSeries.toBoolean()) {
                val showSelection = remember { mutableStateOf(false) }
                val listState = rememberLazyListState()

                ElevatedCard(
                    Modifier.fillMaxSize().padding(12.dp),
                    colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    EpisodeList(
                        storage,
                        mediaStorage,
                        showSelection,
                        null,
                        listState,
                        canShowMediaInfo = false,
                        onPlay = { entry ->
                            nav.push(PlayerView(entry.GUID, storage.progressFor(entry)))
                            ""
                        }
                    ) {
                        TvDetailSidebar(
                            mediaStorage = mediaStorage,
                            storage = storage,
                            sm = sm,
                            modifier = Modifier.fillMaxWidth(),
                            scrollable = false,
                            footerText = "Continue down for episodes."
                        )
                        HorizontalDivider(Modifier.fillMaxWidth().padding(18.dp, 0.dp, 18.dp, 8.dp))
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ElevatedCard(
                        Modifier.fillMaxWidth().weight(1f),
                        colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                    ) {
                        TvDetailSidebar(
                            mediaStorage = mediaStorage,
                            storage = storage,
                            sm = sm,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TvDetailSidebar(
        mediaStorage: MediaStorage,
        storage: MainDataViewModelStorage,
        sm: MainDataViewModel,
        modifier: Modifier = Modifier,
        scrollable: Boolean = true,
        footerText: String? = null
    ) {
        val nav = LocalGlobalNavigator.current
        val scrollState = rememberScrollState()
        val preferGerman = settings["prefer-german-metadata", false]
        val synopsis = remember(mediaStorage, preferGerman) {
            if (!mediaStorage.media.synopsisDE.isNullOrBlank() && preferGerman) {
                mediaStorage.media.synopsisDE
            } else {
                mediaStorage.media.synopsisEN
            }?.removeSomeHTMLTags()
        }
        val playLabel = remember(mediaStorage, storage.updated) { storage.playLabel(mediaStorage) }

        Column(
            modifier
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StupidImageNameArea(
                mediaStorage,
                modifier = Modifier.fillMaxWidth(),
                dynamicMaxWidth = 1200.dp,
                requiredMinHeight = 180.dp,
                requiredMaxHeight = 280.dp,
                requiredMaxWidth = 180.dp,
                enforceConstraints = true,
                showMappingIcons = false
            ) {
                Text("About", Modifier.padding(6.dp, 2.dp), style = MaterialTheme.typography.titleLarge)
                MediaGenreListing(mediaStorage.media)
                if (!synopsis.isNullOrBlank()) {
                    Text(
                        synopsis,
                        modifier = Modifier.padding(6.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            TvPrimaryActionButton(playLabel) {
                val entry = storage.entryToPlay(mediaStorage) ?: return@TvPrimaryActionButton
                nav.push(PlayerView(entry.GUID, storage.progressFor(entry)))
            }

            TvSecondaryActionButton("Mark As Watched") {
                val job = RequestQueue.addMultipleWatched(mediaStorage.entries)
                sm.screenModelScope.launch {
                    job.join()
                    sm.updateData(true).join()
                }
            }

            TvOutlinedActionButton("Mark As Unwatched") {
                val job = RequestQueue.removeMultipleWatched(mediaStorage.entries)
                sm.screenModelScope.launch {
                    job.join()
                    sm.updateData(true).join()
                }
            }

            if (mediaStorage.prequel != null || mediaStorage.sequel != null) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            "Related",
                            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        MediaRelations(mediaStorage) { nav.pushMediaView(it) }
                    }
                }
            }

            if (!footerText.isNullOrBlank()) {
                Text(
                    footerText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun TvPrimaryActionButton(text: String, onClick: () -> Unit) {
        var isFocused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(if (isFocused) 1.025f else 1f, label = "tv-primary-action-scale")

        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
                .scale(scale)
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    3.dp,
                    if (isFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    AppShapes.large
                )
                .heightIn(min = 48.dp),
            shape = AppShapes.large,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isFocused) 8.dp else 1.dp)
        ) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    private fun TvSecondaryActionButton(text: String, onClick: () -> Unit) {
        var isFocused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "tv-secondary-action-scale")

        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
                .scale(scale)
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    3.dp,
                    if (isFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    AppShapes.large
                )
                .heightIn(min = 48.dp),
            shape = AppShapes.large,
            elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = if (isFocused) 6.dp else 0.dp)
        ) {
            Text(text, fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium)
        }
    }

    @Composable
    private fun TvOutlinedActionButton(text: String, onClick: () -> Unit) {
        var isFocused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "tv-outline-action-scale")

        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
                .scale(scale)
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    3.dp,
                    if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    AppShapes.large
                )
                .heightIn(min = 48.dp),
            shape = AppShapes.large,
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Text(text, fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}
