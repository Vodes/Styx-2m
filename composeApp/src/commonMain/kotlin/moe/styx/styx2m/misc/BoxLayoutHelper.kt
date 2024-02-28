package moe.styx.styx2m.misc

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class LayoutSizes(val width: Dp, val height: Dp) {
    val isLandScape = width > height
    val isMedium = width > 600.dp

    val isWide = isMedium || isLandScape
}

@Composable
fun BoxWithConstraintsScope.fetchSizes() = LayoutSizes(maxWidth, maxHeight)