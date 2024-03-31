package moe.styx.styx2m.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.data.Media
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.eqI
import moe.styx.styx2m.components.PlayerIconButton
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.misc.Track
import moe.styx.styx2m.misc.ifInvalid
import moe.styx.styx2m.misc.secondsDurationString
import moe.styx.styx2m.theme.DarkColorScheme
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
        Text(renderedTitle, style = MaterialTheme.typography.bodyLarge, color = DarkColorScheme.onSurface, modifier = Modifier.weight(1f))
        Box {
            if (trackList.isNotEmpty()) {
                IconButtonWithTooltip(
                    Icons.AutoMirrored.Filled.ListAlt,
                    "Track Selection",
                    Modifier.padding(10.dp, 0.dp).size(70.dp)
                ) { showTrackSelect = !showTrackSelect }
            }
            DropdownMenu(
                showTrackSelect,
                onDismissRequest = { showTrackSelect = false },
                modifier = Modifier.fillMaxHeight(0.7F).heightIn(100.dp, 300.dp).align(Alignment.TopEnd)
            ) {
                Column(Modifier.width(IntrinsicSize.Max).padding(3.dp), horizontalAlignment = Alignment.Start) {
                    Text("Audio Tracks", Modifier.padding(4.dp, 0.dp, 0.dp, 6.dp), style = MaterialTheme.typography.titleMedium)
                    trackList.filter { it.type eqI "audio" }.forEachIndexed { index, track ->
                        TrackDropdownItem(track, mediaPlayer, index != 0)
                    }
                    Text("Subtitle Tracks", Modifier.padding(4.dp, 8.dp, 0.dp, 6.dp), style = MaterialTheme.typography.titleMedium)
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
        contentColor = DarkColorScheme.onSurface,
        containerColor = DarkColorScheme.background.copy(0.5F),
        disabledContentColor = DarkColorScheme.inverseOnSurface
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
            CircularProgressIndicator(modifier = Modifier.padding(14.dp).requiredSize(60.dp), color = MaterialTheme.colorScheme.secondary)
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