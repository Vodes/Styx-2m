package moe.styx.styx2m.views.settings

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun exitApp() {
    val activity = (LocalContext.current as? Activity)
    activity?.finish()
}