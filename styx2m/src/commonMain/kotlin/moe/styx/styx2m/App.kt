package moe.styx.styx2m

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.multiplatform.lifecycle.LifecycleEvent
import com.multiplatform.lifecycle.LifecycleListener
import com.multiplatform.lifecycle.LifecycleTracker
import com.russhwolf.settings.set
import io.kamel.image.config.LocalKamelConfig
import moe.styx.common.compose.extensions.kamelConfig
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.DownloadQueue
import moe.styx.common.compose.threads.Heartbeats
import moe.styx.common.compose.threads.RequestQueue
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.LocalToaster
import moe.styx.common.util.Log
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.misc.fetchWindowSize
import moe.styx.styx2m.player.PlayerView
import moe.styx.styx2m.theme.AppTheme
import moe.styx.styx2m.theme.LocalThemeIsDark
import moe.styx.styx2m.views.MainOverview

@Composable
internal fun App() = AppTheme {
    val currentSizes = fetchWindowSize()
    settings["is-tablet"] = currentSizes.isProbablyTablet
    InitLifeCycleListener()
    Surface(modifier = Modifier.fillMaxSize()) {
        val darkState = LocalThemeIsDark.current
        val toasterState = rememberToasterState()
        Toaster(toasterState, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 38.dp), darkTheme = darkState.value, richColors = true)
        Navigator(MainOverview()) { navigator ->
            CompositionLocalProvider(
                LocalGlobalNavigator provides navigator,
                LocalKamelConfig provides kamelConfig,
                LocalLayoutSize provides currentSizes,
                LocalToaster provides toasterState
            ) {
                SlideTransition(
                    navigator, animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    ),
                    content = {
                        val mod = if (it is PlayerView) Modifier.fillMaxSize()
                        else Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                        Surface(mod) {
                            it.Content()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InitLifeCycleListener() {
    DisposableEffect(Unit) {
        val listener = object : LifecycleListener {
            override fun onEvent(event: LifecycleEvent) {
                Log.d { "Lifecycle Event: $event" }
                Heartbeats.onLifecycleEvent(event)
                RequestQueue.onLifecycleEvent(event)
                DownloadQueue.onLifecycleEvent(event)
            }
        }
        LifecycleTracker.addListener(listener)
        LifecycleTracker.notifyListeners(LifecycleEvent.OnResumeEvent)
        onDispose {
            LifecycleTracker.removeListener(listener)
        }
    }
}