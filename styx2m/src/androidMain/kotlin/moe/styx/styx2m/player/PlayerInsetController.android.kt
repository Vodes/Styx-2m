package moe.styx.styx2m.player

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import moe.styx.common.util.Log
import moe.styx.styx2m.misc.findActivity

@Composable
actual fun HandlePlayerInsets() {
    val context = LocalContext.current
    val view = LocalView.current
    val window = context.findActivity()?.window
    val controller = remember(window, view) { window?.let { WindowCompat.getInsetsController(window, view) } }

    LaunchedEffect(Unit) {
        window?.let {
            Log.d { "Hiding nav-bar and insets" }
            WindowCompat.setDecorFitsSystemWindows(it, false)
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            Log.d { "Restoring nav-bar and insets" }
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, true)
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}