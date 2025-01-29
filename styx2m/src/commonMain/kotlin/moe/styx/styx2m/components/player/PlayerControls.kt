package moe.styx.styx2m.components.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import moe.styx.common.data.MediaEntry
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.player.MediaPlayer
import moe.styx.styx2m.player.PlaybackStatus
import moe.styx.styx2m.theme.darkScheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50.dp),
    surfacePadding: PaddingValues = PaddingValues(0.dp),
    onLongPress: () -> Unit = {},
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.padding(surfacePadding).wrapContentSize().clip(shape)
            .background(if (enabled) darkScheme.surface.copy(0.6f) else darkScheme.surface.copy(0.3f))
            .combinedClickable(interaction, LocalIndication.current, enabled = enabled, onLongClick = onLongPress, onClick = onClick),
    ) {
        Icon(
            icon,
            contentDescription,
            modifier.align(Alignment.Center),
            tint = if (enabled) darkScheme.onSurface else darkScheme.onSurface.copy(0.5f)
        )
    }
}

@Composable
fun ColumnScope.ControlsRow(
    mediaPlayer: MediaPlayer,
    playbackStatus: PlaybackStatus,
    currentTime: Long,
    chapters: List<Chapter>,
    nextEntry: MediaEntry?,
    previousEntry: MediaEntry?,
    onTap: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val iconsEnabled = playbackStatus !in arrayOf(PlaybackStatus.Idle, PlaybackStatus.EOF)
    Row(
        Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (nextEntry != null || previousEntry != null) {
            PlayerIconButton(
                Modifier.padding(5.dp).requiredSize(39.dp),
                icon = Icons.AutoMirrored.Filled.ArrowLeft,
                "Previous episode",
                surfacePadding = PaddingValues(11.dp)
            ) {
                if (previousEntry != null)
                    mediaPlayer.playEntry(previousEntry, scope)
            }
        }
        PlayerIconButton(
            Modifier.padding(5.dp).requiredSize(50.dp),
            surfacePadding = PaddingValues(11.dp),
            icon = Icons.Default.KeyboardDoubleArrowLeft,
            contentDescription = "Seek backwards",
            enabled = iconsEnabled,
            onLongPress = {
                onTap()
                val validChapter = chapters.sortedBy { it.time }.findLast { it.time < (currentTime - 2) }
                if (chapters.isEmpty() || validChapter == null)
                    return@PlayerIconButton
                mediaPlayer.seek(validChapter.time.toLong())
            }) {
            onTap()
            if (currentTime - 5 < 0)
                return@PlayerIconButton
            mediaPlayer.seek(currentTime - 5)
        }

        if (playbackStatus in arrayOf(PlaybackStatus.Buffering, PlaybackStatus.Seeking)) {
            CircularProgressIndicator(
                modifier = Modifier.padding(11.dp).requiredSize(58.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            PlayerIconButton(
                Modifier.padding(5.dp).requiredSize(50.dp),
                surfacePadding = PaddingValues(11.dp),
                icon = if (playbackStatus is PlaybackStatus.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (playbackStatus is PlaybackStatus.Paused) "Play" else "Pause",
                enabled = iconsEnabled
            ) {
                onTap()
                mediaPlayer.setPlaying(playbackStatus is PlaybackStatus.Paused)
            }
        }

        PlayerIconButton(
            Modifier.padding(5.dp).requiredSize(50.dp),
            icon = Icons.Default.KeyboardDoubleArrowRight,
            surfacePadding = PaddingValues(11.dp),
            contentDescription = "Seek forwards",
            enabled = iconsEnabled,
            onLongPress = {
                onTap()
                val validChapter = chapters.sortedBy { it.time }.find { it.time > currentTime }
                if (chapters.isEmpty() || validChapter == null)
                    return@PlayerIconButton
                mediaPlayer.seek(validChapter.time.toLong())
            }) {
            onTap()
            mediaPlayer.seek(currentTime + 10)
        }

        if (nextEntry != null || previousEntry != null) {
            PlayerIconButton(
                Modifier.padding(5.dp).requiredSize(39.dp),
                icon = Icons.AutoMirrored.Filled.ArrowRight,
                "Next episode",
                surfacePadding = PaddingValues(11.dp)
            ) {
                if (nextEntry != null)
                    mediaPlayer.playEntry(nextEntry, scope)
            }
        }
    }
}