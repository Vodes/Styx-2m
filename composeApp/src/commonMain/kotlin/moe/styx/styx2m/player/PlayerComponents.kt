package moe.styx.styx2m.player

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.Navigator
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.styx2m.misc.secondsDurationString
import moe.styx.styx2m.theme.DarkColorScheme

@Composable
fun NameRow(title: String, media: Media?, entry: MediaEntry?, nav: Navigator) {
    val renderedTitle = if (title.isBlank() || title.contains("?token")) {
        "${media?.name ?: "Unknown"} - ${entry?.entryNumber}"
    } else title
    Row(
        Modifier.fillMaxWidth().background(DarkColorScheme.background.copy(0.5F)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButtonWithTooltip(
            Icons.Default.Close,
            "Back",
            Modifier.size(70.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = DarkColorScheme.onSurface,
                disabledContentColor = DarkColorScheme.inverseOnSurface
            )
        ) { nav.pop() }
        Text(renderedTitle, style = MaterialTheme.typography.bodyLarge, color = DarkColorScheme.onSurface)
    }
}

@Composable
fun ColumnScope.ControlsRow(mediaPlayer: MediaPlayer, playbackStatus: PlaybackStatus, currentTime: Long, onTap: () -> Unit) {
    val iconsEnabled = playbackStatus !in arrayOf(PlaybackStatus.Idle, PlaybackStatus.EOF, PlaybackStatus.Buffering)
    Row(
        Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButtonWithTooltip(
            Icons.Default.KeyboardDoubleArrowLeft,
            "Backwards 10sec",
            Modifier.padding(12.dp).requiredSize(60.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = DarkColorScheme.onSurface,
                containerColor = DarkColorScheme.background.copy(0.5F),
                disabledContentColor = DarkColorScheme.inverseOnSurface
            ),
            enabled = iconsEnabled
        ) {
            onTap()
            if (currentTime - 10 < 0)
                return@IconButtonWithTooltip
            mediaPlayer.seek(currentTime - 10)
        }
        if (playbackStatus in arrayOf(PlaybackStatus.Buffering, PlaybackStatus.Seeking)) {
            CircularProgressIndicator(modifier = Modifier.requiredSize(60.dp).padding(12.dp), color = MaterialTheme.colorScheme.secondary)
        } else {
            IconButtonWithTooltip(
                if (playbackStatus is PlaybackStatus.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                "Play/Pause",
                Modifier.padding(12.dp).requiredSize(60.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = DarkColorScheme.onSurface,
                    containerColor = DarkColorScheme.background.copy(0.5F),
                    disabledContentColor = DarkColorScheme.inverseOnSurface
                ),
                enabled = iconsEnabled
            ) {
                onTap()
                mediaPlayer.setPlaying(playbackStatus is PlaybackStatus.Paused)
            }
        }
        IconButtonWithTooltip(
            Icons.Default.KeyboardDoubleArrowRight,
            "Forwards 10sec",
            Modifier.padding(12.dp).requiredSize(60.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = DarkColorScheme.onSurface,
                containerColor = DarkColorScheme.background.copy(0.5F),
                disabledContentColor = DarkColorScheme.inverseOnSurface
            ),
            enabled = iconsEnabled
        ) {
            onTap()
            mediaPlayer.seek(currentTime + 10)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineControls(mediaPlayer: MediaPlayer, currentTime: Long, cacheTime: Long, duration: Long, onTap: () -> Unit) {
    Row(
        Modifier.padding(25.dp, 20.dp).clip(AppShapes.extraLarge).background(DarkColorScheme.background.copy(0.5F)).fillMaxWidth(0.88F),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var selectedSliderVal by remember { mutableStateOf(0F) }
        val thumbInteractionSource = remember { MutableInteractionSource() }
        Text(currentTime.secondsDurationString(), Modifier.padding(15.dp, 10.dp))
        Box(Modifier.weight(1f, true)) {
            Slider(
                cacheTime.toFloat(),
                {},
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth().height(25.dp).zIndex(1F).clip(AppShapes.medium),
                colors = SliderDefaults.colors(
                    activeTrackColor = DarkColorScheme.onSurface.copy(0.3F),
                    inactiveTrackColor = DarkColorScheme.surface.copy(0.8F)
                ),
                thumb = {}
            )
            Slider(
                if (selectedSliderVal != 0F) selectedSliderVal else currentTime.toFloat(),
                { selectedSliderVal = it; onTap() },
                onValueChangeFinished = {
                    mediaPlayer.seek(selectedSliderVal.toLong())
                    selectedSliderVal = 0F
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth().height(25.dp).zIndex(2F).clip(AppShapes.medium),
                colors = SliderDefaults.colors(
                    activeTrackColor = DarkColorScheme.primary,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = DarkColorScheme.primary
                ),
//                thumb = {
//                    SliderDefaults.Thumb(
//                        thumbInteractionSource,
//                        modifier = Modifier.scale(0.8F, 0.8F),
//                        colors = SliderDefaults.colors(thumbColor = DarkColorScheme.primary),
//                    )
//                }
            )
        }
        Text(duration.secondsDurationString(), Modifier.padding(15.dp, 10.dp))
    }
}