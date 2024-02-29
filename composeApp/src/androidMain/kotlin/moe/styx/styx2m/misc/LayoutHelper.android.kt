package moe.styx.styx2m.misc

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun fetchWindowSize(): LayoutSizes {
    val config = LocalConfiguration.current
    return LayoutSizes(config.screenWidthDp, config.screenHeightDp)
}