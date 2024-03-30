package moe.styx.styx2m.views.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.styx.common.compose.appConfig
import moe.styx.common.compose.threads.DownloadQueue
import moe.styx.common.util.SYSTEMFILES
import okio.Path.Companion.toPath

@Composable
fun AppSettings() {
    var shouldExit by remember { mutableStateOf(false) }
    if (shouldExit)
        exitApp()
    Column(Modifier.padding(2.dp)) {
        Text(
            "Below are some options to \"fix\" unnecessary data usage.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(5.dp)
        )
        Button({
            SYSTEMFILES.deleteRecursively(DownloadQueue.tempDir)
            SYSTEMFILES.createDirectory(DownloadQueue.tempDir)
        }, modifier = Modifier.padding(4.dp)) {
            Text("Delete temp folder")
        }
        Text(
            "This contains unfinished/broken downloads.\nShould be automatically cleaned up on restart but just in case.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(5.dp)
        )
        Button(
            {
                val appDir = appConfig().appStoragePath.toPath()
                val cacheDir = appConfig().appCachePath.toPath()
                SYSTEMFILES.deleteRecursively(appDir)
                SYSTEMFILES.deleteRecursively(cacheDir)
                SYSTEMFILES.createDirectories(appDir)
                SYSTEMFILES.createDirectories(cacheDir)
                shouldExit = true
            },
            modifier = Modifier.padding(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("Delete all files")
        }
        Text(
            "Deletes all the files without actually deleting the login information.\n" +
                    "That's why this is preferable to doing it in the system app settings..\n" +
                    "This will exit the app after.\n" +
                    "This may or may not break the app.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(5.dp)
        )
    }
}

@Composable
expect fun exitApp()