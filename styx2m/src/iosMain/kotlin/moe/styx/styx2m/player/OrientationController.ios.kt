package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults

@Composable
actual fun RequestPlayerRotationLock(locked: Boolean) {
    LaunchedEffect(locked) {
        requestOrientation(locked)
    }

    DisposableEffect(Unit) {
        onDispose {
            requestOrientation(false)
        }
    }
}

private fun requestOrientation(landscape: Boolean) {
    NSUserDefaults.standardUserDefaults.setBool(landscape, forKey = "styx.forceLandscape")
    NSNotificationCenter.defaultCenter.postNotificationName("styx.orientationLockChanged", null)
}
