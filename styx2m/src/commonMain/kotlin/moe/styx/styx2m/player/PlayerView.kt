package moe.styx.styx2m.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.moriatsushi.insetsx.SystemBarsBehavior
import com.moriatsushi.insetsx.rememberWindowInsetsController
import com.multiplatform.lifecycle.LifecycleEvent
import com.multiplatform.lifecycle.LifecycleListener
import com.multiplatform.lifecycle.LifecycleTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import moe.styx.common.compose.threads.Heartbeats
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.common.data.MediaActivity
import moe.styx.common.extension.eqI
import moe.styx.styx2m.misc.*
import kotlin.jvm.Transient

class PlayerView(val entryID: String, startAt: Long = 0L) : Screen {
    @Transient
    var mediaPlayer: MediaPlayer = MediaPlayer(entryID, startAt)

    @Transient
    val listener = object : LifecycleListener {
        override fun onEvent(event: LifecycleEvent) {
            if (event in arrayOf(LifecycleEvent.OnPauseEvent, LifecycleEvent.OnStopEvent))
                mediaPlayer.setPlaying(false)
        }
    }

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val insets = rememberWindowInsetsController()
        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        val storage by sm.storageFlow.collectAsState()
        val (_, mediaStorage) = remember(storage) { sm.getMediaStorageForEntryID(entryID, storage) }

        insets?.setIsNavigationBarsVisible(false)
        insets?.setIsStatusBarsVisible(false)
        insets?.setSystemBarsBehavior(SystemBarsBehavior.Immersive)
        KeepScreenOn()

        LaunchedEffect(Unit) {
            LifecycleTracker.addListener(listener)
        }

        val currentEntryState by mediaPlayer.currentEntry.collectAsState()
        val playbackStatus by mediaPlayer.playbackStatus.collectAsState()

        val playerState by combine(
            mediaPlayer.mediaTitle,
            mediaPlayer.cacheEnd,
            mediaPlayer.progress,
            mediaPlayer.fileLength,
            mediaPlayer.trackList,
            mediaPlayer.chapters
        ) {
            PlayerState(it[0] as String, it[1] as Long, it[2] as Long, it[3] as Long, it[4] as List<Track>, it[5] as List<Chapter>)
        }.collectAsState(PlayerState())

        val currentEntry = remember(currentEntryState) { mediaStorage.entries.find { it.GUID eqI currentEntryState } }
        val next =
            remember(currentEntry, mediaStorage.media) { currentEntry?.let { mediaStorage.entries.findNext(currentEntry, mediaStorage.media) } }
        val prev =
            remember(currentEntry, mediaStorage.media) { currentEntry?.let { mediaStorage.entries.findPrevious(currentEntry, mediaStorage.media) } }

        DisposableEffect(Unit) {
            onDispose {
                LifecycleTracker.removeListener(listener)
                insets?.setIsNavigationBarsVisible(true)
                insets?.setIsStatusBarsVisible(true)
                insets?.setSystemBarsBehavior(SystemBarsBehavior.Default)
                mediaPlayer.releasePlayer()
                Heartbeats.mediaActivity = null
                updateWatchedForID(currentEntryState, playerState.progress, mediaPlayer.playbackPercent)
                sm.updateData(true)
            }
        }

        var controlsTimeout by remember { mutableStateOf(4) }
        val interactionSource = remember { MutableInteractionSource() }

        LaunchedEffect(playerState, playbackStatus, currentEntry) {
            if (currentEntry != null)
                Heartbeats.mediaActivity = MediaActivity(currentEntry.GUID, playerState.progress, playbackStatus == PlaybackStatus.Playing)
        }

        Box(Modifier.fillMaxSize().clickable(interactionSource, indication = null) { controlsTimeout = if (controlsTimeout > 0) 0 else 3 }) {
            Row(Modifier.zIndex(0F).fillMaxSize()) {
                mediaPlayer.PlayerComponent(mediaStorage.entries)
            }
            AnimatedVisibility(controlsTimeout != 0, enter = fadeIn(), exit = fadeOut()) {
                LaunchedEffect(key1 = "") {
                    while (controlsTimeout != 0) {
                        controlsTimeout--
                        if (controlsTimeout < 0)
                            controlsTimeout = 0
                        delay(1000)
                    }
                }

                Column(Modifier.zIndex(1F).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    NameRow(playerState.mediaTitle, mediaStorage.media, currentEntry, nav, playerState.trackList, mediaPlayer) { controlsTimeout = 3 }
                    ControlsRow(mediaPlayer, playbackStatus, playerState.progress, playerState.chapters, next, prev) { controlsTimeout = 4 }
                    TimelineControls(
                        mediaPlayer,
                        playerState.progress,
                        playerState.cacheEnd,
                        playerState.fileLength,
                        playerState.chapters
                    ) { controlsTimeout = 4 }
                }
            }
        }
    }
}