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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.screen.Screen
import com.moriatsushi.insetsx.SystemBarsBehavior
import com.moriatsushi.insetsx.rememberWindowInsetsController
import com.multiplatform.lifecycle.LifecycleEvent
import com.multiplatform.lifecycle.LifecycleListener
import com.multiplatform.lifecycle.LifecycleTracker
import kotlinx.coroutines.delay
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.extension.eqI
import kotlin.jvm.Transient

class PlayerView(val entryID: String) : Screen {
    @Transient
    var mediaPlayer: MediaPlayer = MediaPlayer(entryID)

    @Transient
    val listener = object : LifecycleListener {
        override fun onEvent(event: LifecycleEvent) {
            when (event) {
                in arrayOf(LifecycleEvent.OnPauseEvent, LifecycleEvent.OnStopEvent) -> mediaPlayer.setPlaying(false)
                LifecycleEvent.OnResumeEvent -> mediaPlayer.setPlaying(true)
                else -> {}
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val insets = rememberWindowInsetsController()
        val entryList by Storage.stores.entryStore.getCurrentAndCollectFlow()
        val mediaList by Storage.stores.mediaStore.getCurrentAndCollectFlow()
        insets?.setIsNavigationBarsVisible(false)
        insets?.setIsStatusBarsVisible(false)
        insets?.setSystemBarsBehavior(SystemBarsBehavior.Immersive)

        LaunchedEffect(Unit) {
            LifecycleTracker.addListener(listener)
        }

        DisposableEffect(Unit) {
            onDispose {
                LifecycleTracker.removeListener(listener)
                insets?.setIsNavigationBarsVisible(true)
                insets?.setIsStatusBarsVisible(true)
                insets?.setSystemBarsBehavior(SystemBarsBehavior.Default)
                mediaPlayer.releasePlayer()
            }
        }

        var controlsTimeout by remember { mutableStateOf(4) }
        val interactionSource = remember { MutableInteractionSource() }
        val playbackStatus by mediaPlayer.playbackStatus.collectAsState()
        val currentTime by mediaPlayer.progress.collectAsState()
        val cacheTime by mediaPlayer.cacheEnd.collectAsState()
        val duration by mediaPlayer.fileLength.collectAsState()
        val trackList by mediaPlayer.trackList.collectAsState()
        val chapters by mediaPlayer.chapters.collectAsState()

        Box(Modifier.fillMaxSize().clickable(interactionSource, indication = null) { controlsTimeout = if (controlsTimeout > 0) 0 else 3 }) {
            Row(Modifier.zIndex(0F).fillMaxSize()) {
                mediaPlayer.PlayerComponent()
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
                val currentEntryState by mediaPlayer.currentEntry.collectAsState()
                val currentEntry = entryList.find { it.GUID eqI currentEntryState }
                val media = currentEntry?.let { mediaList.find { it.GUID eqI currentEntry.mediaID } }
                val mediaTitle by mediaPlayer.mediaTitle.collectAsState()

                Column(Modifier.zIndex(1F).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    NameRow(mediaTitle, media, currentEntry, nav)
                    ControlsRow(mediaPlayer, playbackStatus, currentTime) { controlsTimeout = 4 }
                    TimelineControls(mediaPlayer, currentTime, cacheTime, duration, chapters) { controlsTimeout = 4 }
                }
            }
        }
    }
}