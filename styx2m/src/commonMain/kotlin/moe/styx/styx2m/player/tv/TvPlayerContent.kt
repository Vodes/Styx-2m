package moe.styx.styx2m.player.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.darkScheme
import moe.styx.common.compose.viewmodels.MediaStorage
import moe.styx.common.data.MediaEntry
import moe.styx.common.extension.eqI
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.misc.Track
import moe.styx.styx2m.player.MediaPlayer
import moe.styx.styx2m.player.PlaybackStatus
import moe.styx.styx2m.player.PlayerState
import kotlin.time.TimeSource

@Composable
internal fun TvPlayerContent(
    nav: Navigator,
    mediaPlayer: MediaPlayer,
    mediaStorage: MediaStorage,
    playerState: PlayerState,
    playbackStatus: PlaybackStatus,
    currentEntry: MediaEntry?,
    nextEntry: MediaEntry?,
    previousEntry: MediaEntry?
) {
    val scope = rememberCoroutineScope()
    val rootFocusRequester = remember { FocusRequester() }
    val seekBarFocusRequester = remember { FocusRequester() }
    val primaryControlFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    val trackButtonFocusRequester = remember { FocusRequester() }
    val sortedChapters = remember(playerState.chapters) { playerState.chapters.sortedBy(Chapter::time) }
    val audioTracks = remember(playerState.trackList) { playerState.trackList.filter { it.type eqI "audio" } }
    val subtitleTracks = remember(playerState.trackList) { playerState.trackList.filter { it.type eqI "sub" } }
    val hasTracks = audioTracks.isNotEmpty() || subtitleTracks.isNotEmpty()
    val topRowFocusRequester = if (hasTracks) trackButtonFocusRequester else backButtonFocusRequester
    val previousChapter = remember(sortedChapters, playerState.progress) {
        sortedChapters.findLast { it.time < (playerState.progress - 2) }
    }
    val nextChapter = remember(sortedChapters, playerState.progress) {
        sortedChapters.find { it.time > playerState.progress }
    }
    val renderedTitle = remember(playerState.mediaTitle, mediaStorage.media.name, currentEntry?.entryNumber) {
        if (playerState.mediaTitle.isBlank() || playerState.mediaTitle.contains("?token")) {
            "${mediaStorage.media.name} - ${currentEntry?.entryNumber ?: ""}".trimEnd(' ', '-')
        } else {
            playerState.mediaTitle
        }
    }
    val playbackLabel = remember(playbackStatus) {
        when (playbackStatus) {
            PlaybackStatus.Buffering -> "Buffering"
            PlaybackStatus.Seeking -> "Seeking"
            PlaybackStatus.Paused -> "Paused"
            PlaybackStatus.Playing -> "Playing"
            PlaybackStatus.EOF -> "Finished"
            else -> ""
        }
    }

    var controlsVisible by rememberSaveable { mutableStateOf(false) }
    var showTrackSelect by rememberSaveable { mutableStateOf(false) }
    var controlsInteractionTick by remember { mutableStateOf(0) }
    var seekPreviewPosition by remember { mutableStateOf<Long?>(null) }
    var seekPreviewVersion by remember { mutableStateOf(0) }
    var seekDelta by remember { mutableStateOf(0L) }
    var lastSeekDirection by remember { mutableStateOf(0) }
    var lastSeekInteractionAt by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    var seekBurstCount by remember { mutableStateOf(0) }
    var seekBarFocused by remember { mutableStateOf(false) }

    fun bumpControlsTimer() {
        controlsInteractionTick += 1
    }

    fun requestFocus(focusRequester: FocusRequester) {
        scope.launch {
            delay(32)
            focusRequester.requestFocus()
        }
    }

    fun revealControls(focusRequester: FocusRequester = primaryControlFocusRequester) {
        controlsVisible = true
        bumpControlsTimer()
        requestFocus(focusRequester)
    }

    fun hideControls() {
        showTrackSelect = false
        controlsVisible = false
        bumpControlsTimer()
        requestFocus(rootFocusRequester)
    }

    fun clampSeek(position: Long): Long {
        if (playerState.fileLength <= 0) return position.coerceAtLeast(0)
        val maxPosition = (playerState.fileLength - 1).coerceAtLeast(0)
        return position.coerceIn(0, maxPosition)
    }

    fun resetSeekPreview() {
        seekPreviewPosition = null
        seekDelta = 0L
        seekBurstCount = 0
        lastSeekDirection = 0
        lastSeekInteractionAt = TimeSource.Monotonic.markNow()
    }

    fun currentSeekStep(): Long {
        val duration = playerState.fileLength.coerceAtLeast(0)
        return when {
            duration <= 0L -> 10L
            duration < 30L * 60L -> 10L
            duration < 90L * 60L -> 30L
            else -> 60L
        }
    }

    fun currentSeekMultiplier(): Long {
        return when {
            seekBurstCount < 2 -> 1L
            seekBurstCount < 4 -> 3L
            seekBurstCount < 7 -> 6L
            else -> 12L
        }
    }

    fun queueSeek(direction: Int) {
        if (direction == lastSeekDirection && lastSeekInteractionAt.elapsedNow().inWholeMilliseconds <= 850L) {
            seekBurstCount += 1
        } else {
            seekBurstCount = 0
        }
        lastSeekDirection = direction
        lastSeekInteractionAt = TimeSource.Monotonic.markNow()

        val basePosition = seekPreviewPosition ?: playerState.progress
        val nextPosition = clampSeek(basePosition + (currentSeekStep() * currentSeekMultiplier() * direction))
        seekPreviewPosition = nextPosition
        seekDelta = nextPosition - playerState.progress
        seekPreviewVersion += 1
        bumpControlsTimer()
    }

    LaunchedEffect(Unit) {
        delay(100)
        rootFocusRequester.requestFocus()
    }

    LaunchedEffect(controlsVisible, showTrackSelect, controlsInteractionTick) {
        if (!controlsVisible || showTrackSelect) return@LaunchedEffect
        delay(3200)
        controlsVisible = false
        requestFocus(rootFocusRequester)
    }

    LaunchedEffect(seekPreviewVersion) {
        val targetPosition = seekPreviewPosition ?: return@LaunchedEffect
        delay(260)
        if (seekPreviewPosition == targetPosition) {
            mediaPlayer.seek(targetPosition)
        }
    }

    LaunchedEffect(seekPreviewVersion, controlsVisible) {
        val previewVersion = seekPreviewVersion
        val targetPosition = seekPreviewPosition ?: return@LaunchedEffect
        delay(if (controlsVisible) 1800 else 1000)
        if (seekPreviewVersion == previewVersion && seekPreviewPosition == targetPosition) {
            resetSeekPreview()
        }
    }

    LaunchedEffect(currentEntry?.GUID) {
        resetSeekPreview()
    }

    Box(
        Modifier.fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable(enabled = !showTrackSelect, interactionSource = remember { MutableInteractionSource() })
            .onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false

                if (showTrackSelect) {
                    return@onPreviewKeyEvent when (it.key) {
                        Key.Back, Key.Escape -> {
                            showTrackSelect = false
                            bumpControlsTimer()
                            requestFocus(trackButtonFocusRequester)
                            true
                        }

                        else -> false
                    }
                }

                when (it.key) {
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                        if (!controlsVisible) {
                            revealControls()
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionLeft -> {
                        if (!controlsVisible) {
                            queueSeek(-1)
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionRight -> {
                        if (!controlsVisible) {
                            queueSeek(1)
                            true
                        } else {
                            false
                        }
                    }

                    Key.MediaRewind, Key.MediaSkipBackward -> {
                        queueSeek(-1)
                        true
                    }

                    Key.MediaFastForward, Key.MediaSkipForward -> {
                        queueSeek(1)
                        true
                    }

                    Key.Captions, Key.MediaAudioTrack -> {
                        if (hasTracks) {
                            controlsVisible = true
                            showTrackSelect = true
                            bumpControlsTimer()
                            true
                        } else {
                            false
                        }
                    }

                    Key.Back, Key.Escape -> {
                        if (controlsVisible) {
                            hideControls()
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
    ) {
        mediaPlayer.PlayerComponent(mediaStorage.entries, mediaStorage.preferences)

        TvSeekPreviewOverlay(
            visible = seekPreviewPosition != null && (!controlsVisible || seekBarFocused),
            delta = seekDelta,
            targetPosition = seekPreviewPosition ?: playerState.progress
        )

        AnimatedVisibility(controlsVisible && !showTrackSelect, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = darkScheme.background.copy(alpha = 0.68f),
                    tonalElevation = 3.dp,
                    shape = AppShapes.extraLarge
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TvPlayerCompactButton(
                            icon = Icons.AutoMirrored.Filled.ArrowLeft,
                            contentDescription = "Back",
                            focusRequester = backButtonFocusRequester,
                            onMoveDown = { seekBarFocusRequester.requestFocus() },
                            onFocused = ::bumpControlsTimer,
                            onClick = { nav.pop() }
                        )
                        Text(
                            text = renderedTitle,
                            modifier = Modifier.weight(1f),
                            color = darkScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasTracks) {
                            TvPlayerCompactButton(
                                icon = Icons.AutoMirrored.Filled.ListAlt,
                                contentDescription = "Tracks",
                                focusRequester = trackButtonFocusRequester,
                                onMoveDown = { seekBarFocusRequester.requestFocus() },
                                onFocused = ::bumpControlsTimer,
                                onClick = {
                                    bumpControlsTimer()
                                    showTrackSelect = true
                                }
                            )
                        }
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp).copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TvPlayerSeekBar(
                            progress = playerState.progress,
                            cacheEnd = playerState.cacheEnd,
                            duration = playerState.fileLength,
                            chapters = sortedChapters,
                            previewPosition = seekPreviewPosition,
                            playbackLabel = playbackLabel,
                            focusRequester = seekBarFocusRequester,
                            onMoveUp = { topRowFocusRequester.requestFocus() },
                            onMoveDown = { primaryControlFocusRequester.requestFocus() },
                            onFocused = {
                                seekBarFocused = true
                                bumpControlsTimer()
                            },
                            onFocusLost = { seekBarFocused = false },
                            onSeekBackward = { queueSeek(-1) },
                            onSeekForward = { queueSeek(1) }
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TvPlayerActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.AutoMirrored.Filled.ArrowLeft,
                                label = "Prev",
                                enabled = previousEntry != null,
                                onMoveUp = { seekBarFocusRequester.requestFocus() },
                                onFocused = ::bumpControlsTimer,
                                onClick = {
                                    if (previousEntry != null) {
                                        bumpControlsTimer()
                                        mediaPlayer.playEntry(previousEntry, scope)
                                    }
                                }
                            )
                            TvPlayerActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.KeyboardDoubleArrowLeft,
                                label = "Chapter",
                                enabled = previousChapter != null,
                                onMoveUp = { seekBarFocusRequester.requestFocus() },
                                onFocused = ::bumpControlsTimer,
                                onClick = {
                                    previousChapter?.let {
                                        bumpControlsTimer()
                                        mediaPlayer.seek(it.time.toLong())
                                    }
                                }
                            )
                            TvPlayerActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.KeyboardDoubleArrowLeft,
                                label = "-5s",
                                enabled = playerState.progress > 0,
                                onMoveUp = { seekBarFocusRequester.requestFocus() },
                                onFocused = ::bumpControlsTimer,
                                onClick = {
                                    bumpControlsTimer()
                                    mediaPlayer.seek(clampSeek(playerState.progress - 5))
                                }
                            )
                            TvPlayerActionButton(
                                modifier = Modifier.weight(1.02f),
                                icon = if (playbackStatus == PlaybackStatus.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                label = if (playbackStatus == PlaybackStatus.Paused) "Play" else "Pause",
                                enabled = playbackStatus !in arrayOf(PlaybackStatus.Idle, PlaybackStatus.EOF),
                                focusRequester = primaryControlFocusRequester,
                                loading = playbackStatus in arrayOf(PlaybackStatus.Buffering, PlaybackStatus.Seeking),
                                onMoveUp = { seekBarFocusRequester.requestFocus() },
                                onFocused = ::bumpControlsTimer,
                                onClick = {
                                    bumpControlsTimer()
                                    mediaPlayer.setPlaying(playbackStatus == PlaybackStatus.Paused)
                                }
                            )
                            TvPlayerActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.KeyboardDoubleArrowRight,
                                label = "+10s",
                                enabled = playerState.fileLength <= 0 || playerState.progress < playerState.fileLength - 1,
                                onMoveUp = { seekBarFocusRequester.requestFocus() },
                                onFocused = ::bumpControlsTimer,
                                onClick = {
                                    bumpControlsTimer()
                                    mediaPlayer.seek(clampSeek(playerState.progress + 10))
                                }
                            )
                            TvPlayerActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.KeyboardDoubleArrowRight,
                                label = "Chapter",
                                enabled = nextChapter != null,
                                onMoveUp = { seekBarFocusRequester.requestFocus() },
                                onFocused = ::bumpControlsTimer,
                                onClick = {
                                    nextChapter?.let {
                                        bumpControlsTimer()
                                        mediaPlayer.seek(it.time.toLong())
                                    }
                                }
                            )
                            TvPlayerActionButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.AutoMirrored.Filled.ArrowRight,
                                label = "Next",
                                enabled = nextEntry != null,
                                onMoveUp = { seekBarFocusRequester.requestFocus() },
                                onFocused = ::bumpControlsTimer,
                                onClick = {
                                    if (nextEntry != null) {
                                        bumpControlsTimer()
                                        mediaPlayer.playEntry(nextEntry, scope)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showTrackSelect) {
            TvTrackSelectionOverlay(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                mediaPlayer = mediaPlayer,
                onDismiss = {
                    showTrackSelect = false
                    bumpControlsTimer()
                    requestFocus(topRowFocusRequester)
                }
            )
        }
    }
}
