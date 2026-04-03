package moe.styx.styx2m.player.tv

import SeekerDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.darkScheme
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.misc.handleDPadKeyEvents
import moe.styx.styx2m.misc.ifInvalid
import moe.styx.styx2m.misc.secondsDurationString
import moe.styx.styx2m.player.seeker.Seeker
import moe.styx.styx2m.player.seeker.Segment
import moe.styx.styx2m.player.seeker.rememberSeekerState

@Composable
internal fun TvSeekPreviewOverlay(
    visible: Boolean,
    delta: Long,
    targetPosition: Long
) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        ElevatedCard(
            modifier = Modifier.padding(bottom = 88.dp),
            colors = CardDefaults.elevatedCardColors(
                MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp).copy(alpha = 0.96f)
            )
        ) {
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = when {
                        delta > 0 -> "+${delta.secondsDurationString()}"
                        delta < 0 -> "-${(-delta).secondsDurationString()}"
                        else -> targetPosition.secondsDurationString()
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Target ${targetPosition.secondsDurationString()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = darkScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
internal fun TvPlayerSeekBar(
    progress: Long,
    cacheEnd: Long,
    duration: Long,
    chapters: List<Chapter>,
    previewPosition: Long?,
    playbackLabel: String,
    focusRequester: FocusRequester,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onFocused: () -> Unit,
    onFocusLost: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.005f else 1f, label = "tv-player-seek-scale")
    val interactionSource = remember { MutableInteractionSource() }
    val seekerState = rememberSeekerState()
    val safeDuration = duration.coerceAtLeast(1)
    val displayPosition = previewPosition ?: progress
    val displayDelta = displayPosition - progress
    val segments = remember(chapters, safeDuration) {
        chapters.filter { it.time in 0f..safeDuration.toFloat().ifInvalid(1f) }.map {
            Segment(it.title, it.time, Color.Unspecified)
        }.toMutableList().apply {
            if (isNotEmpty() && first().start != 0f) {
                val first = first()
                if (first.start > 2f) add(0, Segment("", 0f)) else set(0, Segment(first.name, 0f))
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth()
            .scale(scale)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused() else onFocusLost()
            }
            .handleDPadKeyEvents(
                onLeft = onSeekBackward,
                onRight = onSeekForward,
                onUp = onMoveUp,
                onDown = onMoveDown,
                onEnter = onFocused
            )
            .focusable(interactionSource = interactionSource)
            .border(
                1.25.dp,
                if (isFocused) MaterialTheme.colorScheme.primary else darkScheme.outline.copy(alpha = 0.24f),
                AppShapes.large
            ),
        shape = AppShapes.large,
        color = darkScheme.surface.copy(alpha = 0.82f),
        tonalElevation = if (isFocused) 5.dp else 1.dp,
        shadowElevation = if (isFocused) 6.dp else 0.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayPosition.secondsDurationString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = darkScheme.onSurface
                )
                Text(
                    text = when {
                        displayDelta > 0 -> "Seek +${displayDelta.secondsDurationString()}"
                        displayDelta < 0 -> "Seek -${(-displayDelta).secondsDurationString()}"
                        playbackLabel.isNotBlank() -> playbackLabel
                        else -> "Seek"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else darkScheme.onSurface.copy(alpha = 0.72f)
                )
                Text(
                    text = duration.secondsDurationString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = darkScheme.onSurface
                )
            }

            Seeker(
                modifier = Modifier.fillMaxWidth(),
                state = seekerState,
                value = progress.toFloat().ifInvalid(0f),
                thumbValue = displayPosition.toFloat().ifInvalid(0f),
                range = 0f..safeDuration.toFloat().ifInvalid(1f),
                readAheadValue = cacheEnd.toFloat().ifInvalid(0f),
                onValueChange = {},
                segments = segments,
                enabled = false,
                colors = SeekerDefaults.seekerColors(
                    progressColor = MaterialTheme.colorScheme.primary,
                    trackColor = darkScheme.surfaceVariant.copy(alpha = 0.75f),
                    disabledProgressColor = MaterialTheme.colorScheme.primary,
                    disabledTrackColor = darkScheme.surfaceVariant.copy(alpha = 0.75f),
                    thumbColor = MaterialTheme.colorScheme.primary,
                    disabledThumbColor = MaterialTheme.colorScheme.primary,
                    readAheadColor = darkScheme.onSurface.copy(alpha = 0.18f)
                ),
                dimensions = SeekerDefaults.seekerDimensions(
                    trackHeight = 3.dp,
                    progressHeight = 3.dp,
                    thumbRadius = 5.dp,
                    gap = 2.dp
                )
            )

            AnimatedVisibility(displayDelta != 0L, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Left/Right seek, Up for title actions",
                        style = MaterialTheme.typography.labelSmall,
                        color = darkScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
            }
        }
    }
}
