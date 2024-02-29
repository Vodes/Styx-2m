package moe.styx.styx2m.player

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.core.screen.Screen
import com.moriatsushi.insetsx.SystemBarsBehavior
import com.moriatsushi.insetsx.rememberWindowInsetsController

class PlayerView : Screen {

    @Composable
    override fun Content() {
        val insets = rememberWindowInsetsController()
        LaunchedEffect(Unit) {
            insets?.setIsNavigationBarsVisible(false)
            insets?.setIsStatusBarsVisible(false)
            insets?.setSystemBarsBehavior(SystemBarsBehavior.Immersive)
        }
        DisposableEffect(Unit) {
            onDispose {
                insets?.setIsNavigationBarsVisible(true)
                insets?.setIsStatusBarsVisible(true)
                insets?.setSystemBarsBehavior(SystemBarsBehavior.Default)
            }
        }
        Column {
            Text("Test")
        }
    }
}