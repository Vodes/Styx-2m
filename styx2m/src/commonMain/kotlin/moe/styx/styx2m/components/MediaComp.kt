package moe.styx.styx2m.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.kamel.core.Resource
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.http.*
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.anime.MediaGenreListing
import moe.styx.common.compose.components.anime.MediaNameListing
import moe.styx.common.compose.components.anime.MediaRelations
import moe.styx.common.compose.components.misc.ExpandableText
import moe.styx.common.compose.extensions.*
import moe.styx.common.compose.settings
import moe.styx.common.data.Media
import moe.styx.common.extension.toBoolean
import moe.styx.styx2m.misc.LayoutSizes
import moe.styx.styx2m.views.anime.AnimeDetailView
import moe.styx.styx2m.views.anime.MovieDetailView


@Composable
fun MetadataArea(media: Media, nav: Navigator, mediaList: List<Media>, nameImageModifier: Modifier = Modifier, layoutSizes: LayoutSizes) = Column {
    StupidImageNameArea(media, nameImageModifier)
    AboutView(media, layoutSizes)
    if (!media.sequel.isNullOrBlank() || !media.prequel.isNullOrBlank()) {
        HorizontalDivider(Modifier.fillMaxWidth().padding(10.dp, 4.dp, 10.dp, 5.dp), thickness = 2.dp)
        MediaRelations(media, mediaList) {
            settings["episode-list-index"] = 0
            if (it.isSeries.toBoolean())
                nav.replace(AnimeDetailView(it.GUID))
            else
                nav.replace(MovieDetailView(it.GUID))
        }
    }
}

@Composable
fun AboutView(media: Media, layoutSizes: LayoutSizes) {
    val preferGerman = settings["prefer-german-metadata", false]
    val synopsis = if (!media.synopsisDE.isNullOrBlank() && preferGerman) media.synopsisDE else media.synopsisEN
    Text("About", Modifier.padding(6.dp, 2.dp), style = MaterialTheme.typography.titleLarge)
    MediaGenreListing(media)
    if (!synopsis.isNullOrBlank()) {
        SelectionContainer {
            if (!layoutSizes.isLandScape && !layoutSizes.isMedium)
                ExpandableText(synopsis.removeSomeHTMLTags(), Modifier.padding(6.dp), 4, MaterialTheme.typography.bodyMedium, false)
            else
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