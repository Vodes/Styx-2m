package moe.styx.styx2m

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.multiplatform.lifecycle.LifecycleEvent
import com.multiplatform.lifecycle.LifecycleListener
import com.multiplatform.lifecycle.LifecycleTracker
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.kamel.image.config.LocalKamelConfig
import moe.styx.common.compose.extensions.kamelConfig
import moe.styx.common.compose.navigation.Navigator
import moe.styx.common.compose.navigation.StyxCurrentScreenPredictiveBack
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.DownloadQueue
import moe.styx.common.compose.threads.Heartbeats
import moe.styx.common.compose.threads.RequestQueue
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.LocalIsTv
import moe.styx.common.compose.utils.LocalLayoutSize
import moe.styx.common.compose.utils.LocalToaster
import moe.styx.common.compose.utils.LayoutSizes
import moe.styx.common.util.Log
import moe.styx.styx2m.misc.appContentWindowInsetsPadding
import moe.styx.styx2m.player.PlayerView
import moe.styx.styx2m.theme.AppTheme
import moe.styx.styx2m.theme.LocalThemeIsDark
import moe.styx.styx2m.views.MainOverview
import moe.styx.styx2m.views.tv.TvAnimeOverview
import kotlin.math.roundToInt

@Composable
internal fun App() = AppTheme {
    val baseDensity = LocalDensity.current
    val isTv = settings["is-tv", false]
    InitLifeCycleListener()
    Surface(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val currentSizes = LayoutSizes(maxWidth.value.roundToInt(), maxHeight.value.roundToInt())
            val appDensity = if (isTv) {
                Density(baseDensity.density * 0.9f, baseDensity.fontScale * 0.92f)
            } else {
                baseDensity
            }
            settings["is-tablet"] = currentSizes.isProbablyTablet

            val darkState = LocalThemeIsDark.current
            val toasterState = rememberToasterState()
            Toaster(toasterState, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 38.dp), darkTheme = darkState.value, richColors = true)
            Navigator(if (isTv) TvAnimeOverview() else MainOverview()) { navigator ->
                CompositionLocalProvider(
                    LocalDensity provides appDensity,
                    LocalGlobalNavigator provides navigator,
                    LocalIsTv provides isTv,
                    LocalKamelConfig provides kamelConfig,
                    LocalLayoutSize provides currentSizes,
                    LocalToaster provides toasterState
                ) {
                    StyxCurrentScreenPredictiveBack(navigator) {
                        Surface(Modifier.fillMaxSize()) {
                            val contentModifier = if (it is PlayerView) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.fillMaxSize().appContentWindowInsetsPadding()
                            }
                            Box(contentModifier) {
                                it.Content()
                            }
                        }
                    }
                }
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
