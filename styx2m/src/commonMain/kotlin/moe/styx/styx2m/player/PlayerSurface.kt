package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import moe.styx.common.data.MediaPreferences

interface PlayerSurface {
    @Composable
    fun RenderSurface(modifier: Modifier, preferences: MediaPreferences?)
}

interface PlayerBackendWithSurface : PlayerBackend, PlayerSurface
