package moe.styx.styx2m

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.multiplatform.lifecycle.LifecycleEvent
import com.multiplatform.lifecycle.LifecycleListener
import com.multiplatform.lifecycle.LifecycleTracker
import com.russhwolf.settings.set
import com.seiko.imageloader.LocalImageLoader
import io.kamel.image.config.LocalKamelConfig
import moe.styx.common.compose.extensions.getImageLoader
import moe.styx.common.compose.extensions.kamelConfig
import moe.styx.common.compose.http.isLoggedIn
import moe.styx.common.compose.http.login
import moe.styx.common.compose.settings
import moe.styx.common.compose.threads.Heartbeats
import moe.styx.common.compose.threads.RequestQueue
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.Log
import moe.styx.common.compose.utils.ServerStatus
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.misc.fetchWindowSize
import moe.styx.styx2m.theme.AppTheme
import moe.styx.styx2m.views.misc.LoadingView
import moe.styx.styx2m.views.misc.LoginView
import moe.styx.styx2m.views.misc.OfflineView

@Composable
internal fun App() = AppTheme {
    //            var isDark by LocalThemeIsDark.current
    //            IconButton(
    //                onClick = { isDark = !isDark }
    //            ) {
    //                Icon(
    //                    modifier = Modifier.padding(8.dp).size(20.dp),
    //                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
    //                    contentDescription = null
    //                )
    //            }
    val currentSizes = fetchWindowSize()
    settings["is-tablet"] = currentSizes.isProbablyTablet
    val view = if (isLoggedIn()) {
        Log.i { "Logged in as: ${login?.name}" }
        LoadingView()
    } else {
        if (ServerStatus.lastKnown !in listOf(ServerStatus.ONLINE, ServerStatus.UNAUTHORIZED))
            OfflineView()
        else
            LoginView()
    }
    InitLifeCycleListener()
    Surface(modifier = Modifier.fillMaxSize()) {
        Navigator(view) { navigator ->
            CompositionLocalProvider(
                LocalGlobalNavigator provides navigator,
                LocalKamelConfig provides kamelConfig,
                LocalImageLoader provides remember { getImageLoader() },
                LocalLayoutSize provides currentSizes
            ) {
                SlideTransition(
                    navigator, animationSpec = spring(
                        stiffness = Spring.StiffnessMedium,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
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
            }
        }
        LifecycleTracker.addListener(listener)
        LifecycleTracker.notifyListeners(LifecycleEvent.OnResumeEvent)
        onDispose {
            LifecycleTracker.removeListener(listener)
        }
    }
}

internal expect fun openUrl(url: String?)