package moe.styx.styx2m.misc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

data class LayoutSizes(val width: Int, val height: Int) {
    val isLandScape = width > height
    val isMedium = width > 600

    val isWide = isMedium || isLandScape

    val isProbablyTablet: Boolean
        get() {
            return if (isLandScape) height > 600
            else width > 600
        }
}

@Composable
expect fun KeepScreenOn()

@Composable
expect fun fetchWindowSize(): LayoutSizes

val LocalLayoutSize: ProvidableCompositionLocal<LayoutSizes> =
    staticCompositionLocalOf { error("LayoutSizes not initialized") }