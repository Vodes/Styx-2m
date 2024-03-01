package moe.styx.styx2m.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.Navigator
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.misc.ifInvalid
import moe.styx.styx2m.misc.secondsDurationString
import moe.styx.styx2m.theme.DarkColorScheme
import kotlin.math.max
import kotlin.math.min

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

@Composable
fun TimelineControls(
    mediaPlayer: MediaPlayer,
    currentTime: Long,
    cacheTime: Long,
    duration: Long,
    chapters: List<Chapter>,
    onTap: () -> Unit
) {
    Row(
        Modifier.padding(25.dp, 20.dp).clip(AppShapes.extraLarge).background(DarkColorScheme.background.copy(0.5F))
            .fillMaxWidth(0.88F),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        var currentDragOffset by remember { mutableStateOf(0f) }
        var startingOffset by remember { mutableStateOf(0f) }
        Text(currentTime.secondsDurationString(), Modifier.padding(10.dp, 0.dp, 7.dp, 0.dp))
        BoxWithConstraints(Modifier.padding(20.dp).weight(1f).onGloballyPositioned {
            size = it.size
        }.pointerInput(Unit) {
            detectTapGestures { offset ->
                onTap()
                if (size != IntSize.Zero) {
                    val sizeRatio = offset.x / size.width
                    mediaPlayer.seek(max(min((duration * sizeRatio).toLong(), duration - 1), 0))
                }
            }
            detectHorizontalDragGestures(onDragStart = { startingOffset = it.x }, onDragCancel = {
                onTap()
                currentDragOffset = 0f
                startingOffset = 0f
            }, onDragEnd = {
                val sizeRatio = currentDragOffset / size.width
                mediaPlayer.seek(max(min((duration * sizeRatio).toLong(), duration - 1), 0))
            }) { _, amount ->
                onTap()
                if (size != IntSize.Zero && startingOffset != 0f) {
                    val cur = if (currentDragOffset == 0F) {
                        startingOffset
                    } else currentDragOffset
                    currentDragOffset = cur + amount
                }
            }
        }) {
            LinearProgressIndicator(
                { (cacheTime.toFloat() / duration).ifInvalid(0F) },
                Modifier.fillMaxWidth().height(18.dp).zIndex(1F).clip(AppShapes.small),
                color = DarkColorScheme.onSurface.copy(0.3F),
                trackColor = DarkColorScheme.surface.copy(0.8F)
            )
            LinearProgressIndicator(
                { (currentTime.toFloat() / duration).ifInvalid(0F) },
                Modifier.fillMaxWidth().height(18.dp).zIndex(2F).clip(AppShapes.small),
                color = DarkColorScheme.primary,
                trackColor = Color.Transparent
            )
            if (chapters.isNotEmpty()) {
                Canvas(Modifier.fillMaxWidth().height(20.dp).zIndex(3F).padding(0.dp, 1.dp).clip(AppShapes.small)) {
                    val width = size.width
                    val height = size.height
                    for (chapter in chapters.filter { it.time > 1F }) {
                        val offX = width * (chapter.time / duration.toFloat())
                        drawLine(
                            start = Offset(offX - 1F, 0F),
                            end = Offset(offX - 1F, height.toFloat() - 9F),
                            strokeWidth = 5F,
                            color = DarkColorScheme.secondary
                        )
                    }
                }
            }
        }
        Text(duration.secondsDurationString(), Modifier.padding(7.dp, 0.dp, 10.dp, 0.dp))
    }
}