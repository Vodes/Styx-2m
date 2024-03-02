package moe.styx.styx2m.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    onLongPress: () -> Unit = {},
    onClick: () -> Unit
) {
    Icon(
        icon,
        "",
        modifier.combinedClickable(enabled, onClick = onClick, onLongClick = onLongPress),
        tint = if (enabled) colors.contentColor else colors.disabledContentColor
    )
}