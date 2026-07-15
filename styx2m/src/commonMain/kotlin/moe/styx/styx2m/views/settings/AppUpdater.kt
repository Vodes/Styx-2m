package moe.styx.styx2m.views.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppUpdateControls(requestedVersion: String?, modifier: Modifier)

expect fun cleanupDownloadedUpdates()
