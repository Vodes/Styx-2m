package moe.styx.styx2m.player.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.styx.common.compose.components.darkScheme
import moe.styx.styx2m.misc.Track
import moe.styx.styx2m.player.MediaPlayer

@Composable
internal fun TvTrackSelectionOverlay(
    audioTracks: List<Track>,
    subtitleTracks: List<Track>,
    mediaPlayer: MediaPlayer,
    onDismiss: () -> Unit
) {
    val optionCount = audioTracks.size + 1 + subtitleTracks.size
    val focusRequesters = remember(optionCount) { List(optionCount) { FocusRequester() } }
    val subtitleDisabled = subtitleTracks.none { it.selected }
    val initialFocusIndex = remember(audioTracks, subtitleTracks) {
        val selectedAudio = audioTracks.indexOfFirst { it.selected }
        when {
            selectedAudio >= 0 -> selectedAudio
            subtitleDisabled -> audioTracks.size
            else -> {
                val selectedSubtitle = subtitleTracks.indexOfFirst { it.selected }.coerceAtLeast(0)
                audioTracks.size + 1 + selectedSubtitle
            }
        }
    }

    LaunchedEffect(optionCount) {
        delay(64)
        focusRequesters.getOrNull(initialFocusIndex)?.requestFocus()
    }

    Box(
        Modifier.fillMaxSize()
            .background(darkScheme.background.copy(alpha = 0.84f))
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
            modifier = Modifier.fillMaxWidth(0.5f).heightIn(max = 520.dp),
            shape = moe.styx.common.compose.components.AppShapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tracks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TvPlayerCompactButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close tracks",
                        onClick = onDismiss
                    )
                }

                HorizontalDivider()

                if (audioTracks.isNotEmpty()) {
                    Text("Audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    audioTracks.forEachIndexed { index, track ->
                        TvTrackOptionRow(
                            focusRequester = focusRequesters[index],
                            label = "${track.lang ?: "Unknown"}${if (track.title.isNullOrBlank()) "" else " | ${track.title}"}",
                            selected = track.selected,
                            enabled = true,
                            onSelect = {
                                if (!track.selected) {
                                    mediaPlayer.setAudioTrack(track.id)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }

                Text("Subtitles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                TvTrackOptionRow(
                    focusRequester = focusRequesters[audioTracks.size],
                    label = "Off",
                    selected = subtitleDisabled,
                    enabled = true,
                    leadingIcon = Icons.Default.SubtitlesOff,
                    onSelect = {
                        if (!subtitleDisabled) {
                            mediaPlayer.setSubtitleTrack(-1)
                            onDismiss()
                        }
                    }
                )
                subtitleTracks.forEachIndexed { index, track ->
                    TvTrackOptionRow(
                        focusRequester = focusRequesters[audioTracks.size + 1 + index],
                        label = "${track.lang ?: "Unknown"}${if (track.title.isNullOrBlank()) "" else " | ${track.title}"}",
                            selected = track.selected,
                            enabled = true,
                            onSelect = {
                                if (!track.selected) {
                                    mediaPlayer.setSubtitleTrack(track.id)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
        }
    }
}

@Composable
private fun TvTrackOptionRow(
    focusRequester: FocusRequester,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    leadingIcon: ImageVector? = null,
    onSelect: () -> Unit
) {
    TvPlayerActionSurface(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        highlighted = selected,
        focusRequester = focusRequester,
        onClick = onSelect
    ) { _ ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = darkScheme.onSurface)
            }
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) darkScheme.onSurface else darkScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                Icons.Default.Done,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else darkScheme.onSurface.copy(alpha = 0.18f)
            )
        }
    }
}
