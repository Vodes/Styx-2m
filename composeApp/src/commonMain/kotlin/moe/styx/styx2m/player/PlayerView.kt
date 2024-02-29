package moe.styx.styx2m.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.screen.Screen
import com.moriatsushi.insetsx.SystemBarsBehavior
import com.moriatsushi.insetsx.rememberWindowInsetsController
import com.multiplatform.lifecycle.LifecycleEvent
import com.multiplatform.lifecycle.LifecycleListener
import com.multiplatform.lifecycle.LifecycleTracker
import kotlinx.coroutines.delay
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
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
        val isPlaying by mediaPlayer.isPlaying.collectAsState()

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

                Column(Modifier.zIndex(1F).fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(0.5F)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButtonWithTooltip(Icons.Default.ArrowBackIosNew, "Back", Modifier.size(55.dp)) { nav.pop() }
                        Text("${media?.name ?: "Unknown"} - ${currentEntry?.entryNumber}")
                    }
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButtonWithTooltip(if (!isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause, "Play/Pause", Modifier.size(60.dp)) {
                            controlsTimeout = 3
                            mediaPlayer.setPlaying(!isPlaying)
                        }
                    }
                }
            }
        }
    }

}