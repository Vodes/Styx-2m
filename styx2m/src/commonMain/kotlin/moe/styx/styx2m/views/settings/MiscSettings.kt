package moe.styx.styx2m.views.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.styx.common.compose.AppContextImpl.appConfig
import moe.styx.common.compose.components.misc.ServerSelection
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.threads.DownloadQueue
import moe.styx.common.compose.utils.LocalIsTv
import moe.styx.common.util.SYSTEMFILES
import okio.Path.Companion.toPath

@Composable
fun AppSettings() {
    val isTv = LocalIsTv.current
    var shouldExit by remember { mutableStateOf(false) }
    if (shouldExit)
        exitApp()
    Column(Modifier.padding(2.dp)) {
        Text(
            "Below are some options to \"fix\" unnecessary data usage.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(5.dp)
        )
        SettingsActionButton(
            text = "Delete temp folder",
            isTv = isTv,
            onClick = {
                SYSTEMFILES.deleteRecursively(DownloadQueue.tempDir)
                SYSTEMFILES.createDirectory(DownloadQueue.tempDir)
            }
        )
        Text(
            "This contains unfinished/broken downloads.\nShould be automatically cleaned up on restart but just in case.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(5.dp)
        )
        SettingsActionButton(
            text = "Delete all files",
            isTv = isTv,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            onClick = {
                val appDir = appConfig().appStoragePath.toPath()
                val cacheDir = appConfig().appCachePath.toPath()
                SYSTEMFILES.deleteRecursively(appDir)
                SYSTEMFILES.deleteRecursively(cacheDir)
                SYSTEMFILES.createDirectories(appDir)
                SYSTEMFILES.createDirectories(cacheDir)
                shouldExit = true
            },
        )
        Text(
            "Deletes all the files without actually deleting the login information.\n" +
                    "That's why this is preferable to doing it in the system app settings..\n" +
                    "This will exit the app after.\n" +
                    "This may or may not break the app.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(5.dp)
        )

        HorizontalDivider(Modifier.fillMaxWidth().padding(10.dp, 5.dp))

        ServerSelection()
    }
}

@Composable
private fun SettingsActionButton(
    text: String,
    isTv: Boolean,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val buttonBorderColor = if (isTv && isFocused) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = if (isTv) 0.24f else 0f)
    }
    val buttonBorderWidth = if (isTv && isFocused) 3.dp else if (isTv) 1.dp else 0.dp
    val resolvedContainerColor = if (isTv && !isFocused) {
        Color(
            red = (containerColor.red * 0.82f) + (MaterialTheme.colorScheme.surface.red * 0.18f),
            green = (containerColor.green * 0.82f) + (MaterialTheme.colorScheme.surface.green * 0.18f),
            blue = (containerColor.blue * 0.82f) + (MaterialTheme.colorScheme.surface.blue * 0.18f),
            alpha = 1f
        )
    } else {
        containerColor
    }
    val resolvedContentColor = if (isTv && !isFocused) contentColor.copy(alpha = 0.92f) else contentColor
    val buttonModifier = Modifier
        .fillMaxWidth()
        .padding(4.dp)
        .then(
            if (isTv) {
                Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .border(
                        buttonBorderWidth,
                        buttonBorderColor,
                        AppShapes.large
                    )
                    .heightIn(min = 48.dp)
            } else {
                Modifier
            }
        )

    Button(
        onClick = onClick,
        modifier = buttonModifier,
        shape = AppShapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedContainerColor,
            contentColor = resolvedContentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, focusedElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(text, fontWeight = if (isTv && isFocused) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
expect fun exitApp()
