package moe.styx.styx2m.player.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.darkScheme
import moe.styx.common.compose.extensions.clickableNoIndicator
import moe.styx.styx2m.misc.handleDPadKeyEvents

@Composable
internal fun TvPlayerCompactButton(
    icon: ImageVector,
    contentDescription: String,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onMoveDown: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    TvPlayerActionSurface(
        modifier = Modifier.sizeIn(minWidth = 34.dp, minHeight = 34.dp),
        enabled = enabled,
        highlighted = highlighted,
        focusRequester = focusRequester,
        onDown = onMoveDown,
        onFocused = onFocused,
        fillWidth = false,
        onClick = onClick
    ) { isFocused ->
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = when {
                !enabled -> darkScheme.onSurface.copy(alpha = 0.35f)
                highlighted || isFocused -> MaterialTheme.colorScheme.primary
                else -> darkScheme.onSurface
            }
        )
    }
}

@Composable
internal fun TvPlayerActionButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    focusRequester: FocusRequester? = null,
    loading: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    TvPlayerActionSurface(
        modifier = modifier.heightIn(min = 46.dp),
        enabled = enabled,
        focusRequester = focusRequester,
        onUp = onMoveUp,
        onFocused = onFocused,
        onClick = onClick
    ) { isFocused ->
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 1.75.dp
                )
            } else {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(15.dp),
                    tint = if (enabled) {
                        if (isFocused) MaterialTheme.colorScheme.primary else darkScheme.onSurface
                    } else {
                        darkScheme.onSurface.copy(alpha = 0.35f)
                    }
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) darkScheme.onSurface else darkScheme.onSurface.copy(alpha = 0.35f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun TvPlayerActionSurface(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    focusRequester: FocusRequester? = null,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onFocused: (() -> Unit)? = null,
    fillWidth: Boolean = true,
    onClick: () -> Unit,
    content: @Composable (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "tv-player-control-scale")
    val interactionSource = remember { MutableInteractionSource() }
    val baseModifier = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier

    Surface(
        modifier = baseModifier
            .scale(scale)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused?.invoke()
            }
            .handleDPadKeyEvents(
                onUp = onUp,
                onDown = onDown,
                onEnter = { if (enabled) onClick() }
            )
            .focusable(enabled = enabled, interactionSource = interactionSource)
            .clickableNoIndicator(enabled = enabled) { onClick() }
            .border(
                1.25.dp,
                when {
                    !enabled -> darkScheme.outline.copy(alpha = 0.2f)
                    isFocused -> MaterialTheme.colorScheme.primary
                    highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    else -> darkScheme.outline.copy(alpha = 0.24f)
                },
                AppShapes.large
            ),
        shape = AppShapes.large,
        color = darkScheme.surface.copy(alpha = if (enabled) 0.82f else 0.44f),
        tonalElevation = if (isFocused) 5.dp else 1.dp,
        shadowElevation = if (isFocused) 6.dp else 0.dp
    ) {
        Box(
            modifier = if (fillWidth) Modifier.fillMaxWidth().padding(horizontal = 1.dp) else Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            content(isFocused)
        }
    }
}
