package moe.styx.styx2m.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.eqI
import moe.styx.styx2m.misc.Track
import moe.styx.styx2m.player.MediaPlayer
import moe.styx.styx2m.theme.darkScheme


@Composable
fun NameRow(
    title: String,
    media: Media?,
    entry: MediaEntry?,
    nav: Navigator,
    trackList: List<Track>,
    mediaPlayer: MediaPlayer,
    isLocked: Boolean,
    onLockKeyPressed: () -> Unit,
    onTapped: () -> Unit
) {
    val renderedTitle = if (title.isBlank() || title.contains("?token")) {
        "${media?.name ?: "Unknown"} - ${entry?.entryNumber}"
    } else title

    var showTrackSelect by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showTrackSelect) {
        launch {
            while (showTrackSelect) {
                onTapped()
                delay(500)
            }
        }
    }

    Row(
        Modifier.fillMaxWidth().background(darkScheme.background.copy(0.5F)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))
        IconButtonWithTooltip(
            Icons.Default.Close,
            "Back",
            Modifier.size(50.dp).padding(10.dp),
            iconModifier = Modifier.requiredSize(30.dp),
            tint = darkScheme.onSurface,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = darkScheme.onSurface,
                disabledContentColor = darkScheme.inverseOnSurface
            )
        ) { nav.pop() }
        Text(
            renderedTitle,
            style = MaterialTheme.typography.bodyLarge,
            color = darkScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButtonWithTooltip(
            Icons.Default.ScreenLockRotation,
            "Lock rotation",
            Modifier.padding(7.dp, 10.dp),
            iconModifier = Modifier.requiredSize(30.dp),
            tint = if (isLocked)
                darkScheme.primary
            else
                darkScheme.onSurface
        ) {
            onLockKeyPressed()
            onTapped()
        }

        Box {
            if (trackList.isNotEmpty()) {
                IconButtonWithTooltip(
                    Icons.AutoMirrored.Filled.ListAlt,
                    "Track Selection",
                    Modifier.padding(7.dp, 10.dp),
                    iconModifier = Modifier.requiredSize(30.dp),
                    tint = darkScheme.onSurface
                ) { showTrackSelect = !showTrackSelect }
            }
            DropdownMenu(
                showTrackSelect,
                onDismissRequest = { showTrackSelect = false },
                modifier = Modifier.fillMaxHeight(0.7F).heightIn(100.dp, 300.dp).align(Alignment.TopEnd),
                containerColor = darkScheme.surfaceContainer.copy(alpha = 0.85F),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.width(IntrinsicSize.Max).padding(6.dp), horizontalAlignment = Alignment.Start) {
                    Text(
                        "Audio Tracks",
                        Modifier.padding(4.dp, 0.dp, 0.dp, 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = darkScheme.onSurface
                    )
                    trackList.filter { it.type eqI "audio" }.forEachIndexed { index, track ->
                        TrackDropdownItem(track, mediaPlayer, index != 0)
                    }
                    Text(
                        "Subtitle Tracks",
                        Modifier.padding(4.dp, 8.dp, 0.dp, 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = darkScheme.onSurface
                    )
                    trackList.filter { it.type eqI "sub" }.forEachIndexed { index, track ->
                        TrackDropdownItem(track, mediaPlayer, index != 0)
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
    }
}