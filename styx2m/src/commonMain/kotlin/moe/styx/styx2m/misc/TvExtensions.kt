package moe.styx.styx2m.misc

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*

private val DPadEventsKeyCodes = listOf(
    Key.Enter, Key.NumPadEnter, Key.DirectionCenter,
    Key.DirectionRight,
    Key.DirectionLeft,
    Key.DirectionUp,
    Key.DirectionDown
)


fun Modifier.handleDPadKeyEvents(
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null,
) = onPreviewKeyEvent {
    fun onActionUp(block: () -> Unit) {
        if (it.type == KeyEventType.KeyUp) block()
    }

    if (!DPadEventsKeyCodes.contains(it.key)) return@onPreviewKeyEvent false

    when (it.key) {
        Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
            onEnter?.apply {
                onActionUp(::invoke)
                return@onPreviewKeyEvent true
            }
        }

        Key.DirectionLeft -> {
            onLeft?.apply {
                onActionUp(::invoke)
                return@onPreviewKeyEvent true
            }
        }

        Key.DirectionUp -> {
            onUp?.apply {
                onActionUp(::invoke)
                return@onPreviewKeyEvent true
            }
        }

        Key.DirectionDown -> {
            onDown?.apply {
                onActionUp(::invoke)
                return@onPreviewKeyEvent true
            }
        }

        Key.DirectionRight -> {
            onRight?.apply {
                onActionUp(::invoke)
                return@onPreviewKeyEvent true
            }
        }
    }
    false
}
