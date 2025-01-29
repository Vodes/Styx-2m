package moe.styx.styx2m.components.player

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.styx2m.misc.LocalLayoutSize

@Composable
fun PlayerControlsSurface(
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onChapterSkipForward: () -> Boolean = { false },
    onChapterSkipBackward: () -> Boolean = { false },
    onSeekForward: () -> Boolean = { false },
    onSeekBackward: () -> Boolean = { false },
    content: @Composable BoxScope.() -> Unit
) {
    val indicator = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val sizes = LocalLayoutSize.current
    val currentSize = remember(sizes.isLandScape) { IntSize(sizes.width, sizes.height) }
    Box(modifier.fillMaxSize().indication(indicator, LocalIndication.current).pointerInput(currentSize) {
        detectTapGestures(onDoubleTap = {
            val offsetSize = IntSize(it.x.toDp().roundToPx(), it.y.toDp().roundToPx())
            val success = if (offsetSize.width < (currentSize.width / 2))
                onSeekBackward()
            else
                onSeekForward()
            if (success)
                scope.launch {
                    val press = PressInteraction.Press(it)
                    indicator.emit(press)
                    delay(40)
                    indicator.emit(PressInteraction.Release(press))
                }
        }, onLongPress = {
            val offsetSize = IntSize(it.x.toDp().roundToPx(), it.y.toDp().roundToPx())
            val success = if (offsetSize.width < (currentSize.width / 2))
                onChapterSkipBackward()
            else
                onChapterSkipForward()
            if (success)
                scope.launch {
                    val press = PressInteraction.Press(it)
                    indicator.emit(press)
                    delay(40)
                    indicator.emit(PressInteraction.Release(press))
                }
        }, onTap = { onTap() })
    }) {
        content()
    }
}
