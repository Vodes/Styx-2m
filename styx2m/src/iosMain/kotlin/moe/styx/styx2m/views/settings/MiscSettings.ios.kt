package moe.styx.styx2m.views.settings

import androidx.compose.runtime.Composable
import kotlin.system.exitProcess

@Composable
actual fun exitApp() {
    exitProcess(0)
}