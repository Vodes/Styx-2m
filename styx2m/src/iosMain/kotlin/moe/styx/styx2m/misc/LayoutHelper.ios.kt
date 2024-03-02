package moe.styx.styx2m.misc

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun fetchWindowSize(): LayoutSizes {
    val windowInfo = LocalWindowInfo.current
    return LayoutSizes(windowInfo.containerSize.width, windowInfo.containerSize.height)
}


@Composable
actual fun KeepScreenOn() {
    // TODO: Figure out how to handle this on iOS
}