package moe.styx.styx2m.views.anime

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import com.russhwolf.settings.get
import io.kamel.core.Resource
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.*
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.anime.EpisodeList
import moe.styx.common.compose.components.anime.MediaGenreListing
import moe.styx.common.compose.components.anime.MediaNameListing
import moe.styx.common.compose.components.anime.MediaRelations
import moe.styx.common.compose.components.buttons.FavouriteIconButton
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.extensions.*
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.Log
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.eqI

class AnimeDetailView(private val mediaID: String) : Screen {

    override val key: ScreenKey
        get() = mediaID

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val mediaList by Storage.stores.mediaStore.getCurrentAndCollectFlow()
        val media = remember { mediaList.find { it.GUID eqI mediaID } }
        if (media == null) {
            nav.pop()
            return
        }
        val entries = fetchEntries()
        Log.d { entries.size.toString() }

        MainScaffold(title = media.name, actions = {
            FavouriteIconButton(media)
        }) {
            val showSelection = remember { mutableStateOf(false) }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                ElevatedCard(Modifier.padding(2.dp).fillMaxSize()) {
                    if (this@BoxWithConstraints.maxWidth < 550.dp) {
                        Column {
                            EpisodeList(entries, showSelection, null, { "" }) {
                                MetadataArea(media, nav, mediaList)
                                Divider(Modifier.fillMaxWidth().padding(10.dp, 8.dp), thickness = 3.dp)
                            }
                        }
                    } else {
                        val scrollState = rememberScrollState()
                        Row {
                            Column(Modifier.weight(0.5F).verticalScroll(scrollState)) {
                                MetadataArea(media, nav, mediaList, Modifier.fillMaxHeight(0.6F).heightIn(0.dp, 500.dp))
                            }
                            Divider(Modifier.padding(2.dp, 8.dp).fillMaxHeight().width(3.dp))
                            Column(Modifier.weight(0.5F)) {
                                EpisodeList(entries, showSelection, null, { "" })
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun fetchEntries(): List<MediaEntry> {
        val flow by Storage.stores.entryStore.getCurrentAndCollectFlow()
        val filtered = flow.filter { it.mediaID eqI mediaID }
        return if (settings["episode-asc", false]) filtered.sortedBy {
            it.entryNumber.toDoubleOrNull() ?: 0.0
        } else filtered.sortedByDescending { it.entryNumber.toDoubleOrNull() ?: 0.0 }
    }
}

@Composable
fun MetadataArea(media: Media, nav: Navigator, mediaList: List<Media>, nameImageModifier: Modifier = Modifier) = Column {
    StupidImageNameArea(media, nameImageModifier)
    AboutView(media)
    if (!media.sequel.isNullOrBlank() || !media.prequel.isNullOrBlank()) {
        Divider(Modifier.fillMaxWidth().padding(10.dp, 4.dp, 10.dp, 5.dp), thickness = 2.dp)
        MediaRelations(media, mediaList) { nav.replace(AnimeDetailView(it.GUID)) }
    }
}

@Composable
fun AboutView(media: Media) {
    val preferGerman = settings["prefer-german-metadata", false]
    val synopsis = if (!media.synopsisDE.isNullOrBlank() && preferGerman) media.synopsisDE else media.synopsisEN
    Text("About", Modifier.padding(6.dp, 2.dp), style = MaterialTheme.typography.titleLarge)
    MediaGenreListing(media)
    if (!synopsis.isNullOrBlank()) {
        SelectionContainer {
            Text(synopsis.removeSomeHTMLTags(), Modifier.padding(6.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun StupidImageNameArea(
    media: Media,
    modifier: Modifier = Modifier,
    dynamicMaxWidth: Dp = 760.dp,
    requiredWidth: Dp = 385.dp,
    requiredHeight: Dp = 535.dp,
    otherContent: @Composable () -> Unit = {}
) {
    val img = media.getThumb()!!
    val painter = if (img.isCached()) {
        asyncPainterResource("file:/${img.getPath()}", key = img.GUID, filterQuality = FilterQuality.Low)
    } else asyncPainterResource(Url(img.getURL()), key = img.GUID, filterQuality = FilterQuality.Low)
    BoxWithConstraints(modifier) {
        val width = this.maxWidth
        Row(Modifier.align(Alignment.TopStart).height(IntrinsicSize.Max).fillMaxWidth()) {
            if (width <= dynamicMaxWidth)
                BigScalingCardImage(
                    painter,
                    Modifier.fillMaxWidth().weight(1f, false),
                    cardModifier = Modifier.requiredHeightIn(150.dp, 500.dp).aspectRatio(0.71F)
                )
            else {
                // Theoretical max size that should be reached at this window width
                // Just force to not have layout spacing issues lmao
                BigScalingCardImage(painter, Modifier.fillMaxHeight(), cardModifier = Modifier.aspectRatio(0.71F))
            }
            Column(Modifier.fillMaxWidth().weight(1f, true)) {
                MediaNameListing(media, Modifier.align(Alignment.Start))//, Modifier.weight(0.5F))
                otherContent()
//                Spacer(Modifier.weight(1f, true))
//                MappingIcons(media)
            }
        }
    }
}

@Composable
fun BigScalingCardImage(image: Resource<Painter>, modifier: Modifier = Modifier, cardModifier: Modifier = Modifier) {
    Column(modifier) {
        ElevatedCard(
            cardModifier.align(Alignment.Start).padding(12.dp),
        ) {
            KamelImage(
                image,
                contentDescription = "Thumbnail",
                modifier = Modifier.padding(2.dp).clip(AppShapes.small),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}