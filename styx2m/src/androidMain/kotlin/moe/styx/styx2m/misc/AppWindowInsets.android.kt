package moe.styx.styx2m.misc

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.appContentWindowInsetsPadding(): Modifier {
    return windowInsetsPadding(WindowInsets.safeDrawing)
}
