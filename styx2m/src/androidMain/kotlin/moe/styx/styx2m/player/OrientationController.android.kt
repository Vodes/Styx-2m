package moe.styx.styx2m.player

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import moe.styx.common.util.Log
import moe.styx.styx2m.misc.findActivity

@Composable
actual fun RequestPlayerRotationLock(locked: Boolean) {
    val context = LocalContext.current
    val activity = context.findActivity()
    LaunchedEffect(locked) {
        activity?.requestedOrientation = if (locked) {
            ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        Log.d {
            if (locked) {
                "Requesting locked rotation: ${activity != null}"
            } else {
                "Releasing locked rotation: ${activity != null}"
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            Log.d { "Releasing locked rotation: ${activity != null}" }
        }
    }
}
