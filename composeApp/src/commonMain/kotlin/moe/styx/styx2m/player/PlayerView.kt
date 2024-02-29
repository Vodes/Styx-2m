package moe.styx.styx2m.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.screen.Screen
import com.moriatsushi.insetsx.SystemBarsBehavior
import com.moriatsushi.insetsx.rememberWindowInsetsController
import com.multiplatform.lifecycle.LifecycleEvent
import com.multiplatform.lifecycle.LifecycleListener
import com.multiplatform.lifecycle.LifecycleTracker
import kotlinx.coroutines.delay
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.files.getCurrentAndCollectFlow
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.extension.eqI
import moe.styx.styx2m.misc.ifInvalid
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

        Box(Modifier.fillMaxSize().clickable(interactionSource, indication = null) { controlsTimeout = 3 }) {
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
                val iconsEnabled = playbackStatus !in arrayOf(PlaybackStatus.Idle, PlaybackStatus.EOF, PlaybackStatus.Buffering)

                Column(Modifier.zIndex(1F).fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(0.5F)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButtonWithTooltip(Icons.Default.Close, "Back", Modifier.size(70.dp)) { nav.pop() }
                        Text("${media?.name ?: "Unknown"} - ${currentEntry?.entryNumber}")
                    }
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButtonWithTooltip(
                            Icons.Default.KeyboardDoubleArrowLeft,
                            "Backwards 10sec",
                            Modifier.requiredSize(75.dp),
                            enabled = iconsEnabled
                        ) {
                            controlsTimeout = 3
                            if (currentTime - 10 < 0)
                                return@IconButtonWithTooltip
                            mediaPlayer.seek(currentTime - 10)
                        }
                        if (playbackStatus in arrayOf(PlaybackStatus.Buffering, PlaybackStatus.Seeking)) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        } else {
                            IconButtonWithTooltip(
                                if (playbackStatus is PlaybackStatus.Paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                "Play/Pause",
                                Modifier.requiredSize(80.dp),
                                enabled = iconsEnabled
                            ) {
                                controlsTimeout = 3
                                mediaPlayer.setPlaying(playbackStatus is PlaybackStatus.Paused)
                            }
                        }
                        IconButtonWithTooltip(
                            Icons.Default.KeyboardDoubleArrowRight,
                            "Forwards 10sec",
                            Modifier.requiredSize(75.dp),
                            enabled = iconsEnabled
                        ) {
                            controlsTimeout = 3
                            mediaPlayer.seek(currentTime + 10)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(25.dp, 20.dp), horizontalArrangement = Arrangement.Center) {
                        Box {
                            LinearProgressIndicator(
                                { (cacheTime.toFloat() / duration).ifInvalid(0F) },
                                Modifier.fillMaxWidth(0.7F).height(18.dp).clip(AppShapes.medium).zIndex(1F),
                                trackColor = MaterialTheme.colorScheme.surface.copy(0.8F),
                                color = MaterialTheme.colorScheme.onSurface.copy(0.3F)
                            )
                            LinearProgressIndicator(
                                { (currentTime.toFloat() / duration).ifInvalid(0F) },
                                Modifier.fillMaxWidth(0.7F).height(18.dp).clip(AppShapes.medium).zIndex(2F).pointerInput(Unit) {
                                    detectTapGestures(onPress = {
                                        awaitRelease()

                                        val distance = it.getDistance()
                                        println((duration * distance))
                                        mediaPlayer.seek((duration * distance).toLong())
                                    })
                                },
                                trackColor = Color.Transparent,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

}