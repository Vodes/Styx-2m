package moe.styx.styx2m.player

import SeekerDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.containsAny
import moe.styx.common.extension.eqI
import moe.styx.styx2m.components.PlayerIconButton
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.misc.Track
import moe.styx.styx2m.misc.ifInvalid
import moe.styx.styx2m.misc.secondsDurationString
import moe.styx.styx2m.player.seeker.Seeker
import moe.styx.styx2m.player.seeker.Segment
import moe.styx.styx2m.player.seeker.rememberSeekerState
import moe.styx.styx2m.theme.darkScheme
import kotlin.math.max
import kotlin.math.min

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
        IconButtonWithTooltip(
            Icons.Default.Close,
            "Back",
            Modifier.size(70.dp),
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
            Icons.Default.ScreenLockRotation, "Lock rotation", Modifier.size(70.dp), tint = if (isLocked)
                darkScheme.primary
            else
                darkScheme.onSurface
        ) {
            onLockKeyPressed()
        }

        Box {
            if (trackList.isNotEmpty()) {
                IconButtonWithTooltip(
                    Icons.AutoMirrored.Filled.ListAlt,
                    "Track Selection",
                    Modifier.padding(10.dp, 0.dp).size(70.dp),
                    tint = darkScheme.onSurface
                ) { showTrackSelect = !showTrackSelect }
            }
            DropdownMenu(
                showTrackSelect,
                onDismissRequest = { showTrackSelect = false },
                modifier = Modifier.fillMaxHeight(0.7F).heightIn(100.dp, 300.dp).align(Alignment.TopEnd)
            ) {
                Column(Modifier.width(IntrinsicSize.Max).padding(3.dp), horizontalAlignment = Alignment.Start) {
                    Text(
                        "Audio Tracks",
                        Modifier.padding(4.dp, 0.dp, 0.dp, 6.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    trackList.filter { it.type eqI "audio" }.forEachIndexed { index, track ->
                        TrackDropdownItem(track, mediaPlayer, index != 0)
                    }
                    Text(
                        "Subtitle Tracks",
                        Modifier.padding(4.dp, 8.dp, 0.dp, 6.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    trackList.filter { it.type eqI "sub" }.forEachIndexed { index, track ->
                        TrackDropdownItem(track, mediaPlayer, index != 0)
                    }
                }
            }
        }

    }
}

@Composable
fun TrackDropdownItem(track: Track, mediaPlayer: MediaPlayer, border: Boolean) {
    Column {
        if (border) {
            HorizontalDivider(Modifier.padding(5.dp, 3.dp).fillMaxWidth(), thickness = 2.dp)
        }
        Row(
            Modifier.padding(7.dp, 7.dp).height(IntrinsicSize.Min)
                .clickable(enabled = !track.selected || track.type eqI "sub") {
                    if (track.type eqI "sub")
                        if (track.selected)
                            mediaPlayer.setSubtitleTrack(-1)
                        else
                            mediaPlayer.setSubtitleTrack(track.id)
                    else
                        mediaPlayer.setAudioTrack(track.id)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${track.lang ?: "Unknown"}${if (track.title.isNullOrBlank()) "" else " | ${track.title}"}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Done, "Selected track",
                modifier = Modifier.padding(4.dp, 0.dp).size(20.dp).alpha(if (track.selected) 1f else 0f)
            )
        }
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
    val colors = IconButtonDefaults.iconButtonColors(
        contentColor = darkScheme.onSurface,
        containerColor = darkScheme.background.copy(0.5F),
        disabledContentColor = darkScheme.inverseOnSurface
    )
    val iconsEnabled = playbackStatus !in arrayOf(PlaybackStatus.Idle, PlaybackStatus.EOF, PlaybackStatus.Buffering)
    Row(
        Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (nextEntry != null || previousEntry != null) {
            PlayerIconButton(
                Icons.AutoMirrored.Filled.ArrowBackIos,
                Modifier.padding(14.dp).requiredSize(32.dp),
                enabled = previousEntry != null,
                colors = colors
            ) {
                if (previousEntry != null)
                    mediaPlayer.playEntry(previousEntry, scope)
            }
        }
        PlayerIconButton(
            Icons.Default.KeyboardDoubleArrowLeft,
            Modifier.padding(14.dp).requiredSize(50.dp),
            iconsEnabled,
            colors = colors,
            onLongPress = {
                onTap()
                val validChapter = chapters.sortedBy { it.time }.findLast { it.time < currentTime }
                if (chapters.isEmpty() || validChapter == null)
                    return@PlayerIconButton
                mediaPlayer.seek(validChapter.time.toLong())
            }
        ) {
            onTap()
            if (currentTime - 5 < 0)
                return@PlayerIconButton
            mediaPlayer.seek(currentTime - 5)
        }
        if (playbackStatus in arrayOf(PlaybackStatus.Buffering, PlaybackStatus.Seeking)) {
            CircularProgressIndicator(
                modifier = Modifier.padding(14.dp).requiredSize(60.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            PlayerIconButton(
                if (playbackStatus is PlaybackStatus.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                Modifier.padding(14.dp).requiredSize(50.dp),
                colors = colors,
                enabled = iconsEnabled
            ) {
                onTap()
                mediaPlayer.setPlaying(playbackStatus is PlaybackStatus.Paused)
            }
        }
        PlayerIconButton(
            Icons.Default.KeyboardDoubleArrowRight,
            Modifier.padding(14.dp).requiredSize(50.dp),
            iconsEnabled,
            colors = colors,
            onLongPress = {
                onTap()
                val validChapter = chapters.sortedBy { it.time }.find { it.time > currentTime }
                if (chapters.isEmpty() || validChapter == null)
                    return@PlayerIconButton
                mediaPlayer.seek(validChapter.time.toLong())
            }
        ) {
            onTap()
            mediaPlayer.seek(currentTime + 10)
        }
        if (nextEntry != null || previousEntry != null) {
            PlayerIconButton(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                Modifier.padding(14.dp).requiredSize(32.dp),
                enabled = nextEntry != null,
                colors = colors
            ) {
                if (nextEntry != null)
                    mediaPlayer.playEntry(nextEntry, scope)
            }
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
    val seekerState = rememberSeekerState()
    var isDragging by remember { mutableStateOf(false) }
    var seekerValue by remember { mutableStateOf(0f) }
    Row(
        Modifier.padding(25.dp, 20.dp).clip(AppShapes.extraLarge).background(darkScheme.background.copy(0.5F))
            .fillMaxWidth(0.88F),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isDragging) seekerValue.toLong().secondsDurationString() else currentTime.secondsDurationString(),
            Modifier.padding(10.dp, 0.dp, 7.dp, 0.dp),
            color = darkScheme.onSurface
        )

        Seeker(
            Modifier.padding(20.dp, 5.dp).weight(1f),
            seekerState,
            value = max(currentTime.toFloat(), 0f).ifInvalid(0f),
            thumbValue = if (isDragging) seekerValue else max(currentTime.toFloat(), 0f).ifInvalid(0f),
            range = 0f..duration.toFloat().ifInvalid(1f),
            readAheadValue = cacheTime.toFloat().ifInvalid(0f),
            segments = chapters.map {
                Segment(
                    it.title,
                    it.time,
                    if (it.title.containsAny("op", "intro", "ed", "end", "credits"))
                        darkScheme.secondary
                    else
                        Color.Unspecified
                )
            },
            onValueChange = {
                isDragging = true
                seekerValue = it
                onTap()
            },
            onValueChangeFinished = {
                mediaPlayer.seek(max(min(seekerValue.toLong(), duration - 1), 0))
                isDragging = false
            },
            colors = SeekerDefaults.seekerColors(
                darkScheme.primary,
                darkScheme.surface.copy(0.8F),
                thumbColor = darkScheme.primary,
                readAheadColor = darkScheme.onSurface.copy(0.3F)
            )
        )

        Text(duration.secondsDurationString(), Modifier.padding(7.dp, 0.dp, 10.dp, 0.dp), color = darkScheme.onSurface)
    }
}