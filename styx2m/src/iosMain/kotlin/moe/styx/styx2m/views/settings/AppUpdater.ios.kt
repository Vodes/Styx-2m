package moe.styx.styx2m.views.settings

import Styx2m.styx2m.BuildConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.styx.common.compose.http.Endpoints
import moe.styx.common.compose.http.login
import moe.styx.common.http.DownloadResult
import moe.styx.common.http.downloadFileStream
import moe.styx.common.util.Log
import moe.styx.common.util.SYSTEMFILES
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.*
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad

@Composable
actual fun AppUpdateControls(requestedVersion: String?, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val progressFlow = remember { MutableStateFlow(0) }
    val progress by progressFlow.collectAsState()
    var state by remember { mutableStateOf<IosUpdateState>(IosUpdateState.Idle) }
    val isBusy = state is IosUpdateState.Downloading

    Column(modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Current version: ${BuildConfig.APP_VERSION}", style = MaterialTheme.typography.bodyMedium)
        if (requestedVersion != null)
            Text("Available version: $requestedVersion", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Downloads the IPA into Styx2m's Documents folder so it can be selected from Files or shared to a sideloading app.",
            style = MaterialTheme.typography.labelMedium
        )
        Button(
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    progressFlow.value = 0
                    state = IosUpdateState.Downloading
                    state = withContext(Dispatchers.Default) {
                        downloadIpa(requestedVersion) { progressFlow.value = it }
                    }
                }
            }
        ) {
            Text("Download IPA")
        }
        when (val current = state) {
            IosUpdateState.Downloading -> {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Downloading IPA: $progress%")
            }

            is IosUpdateState.Downloaded -> {
                Text("Downloaded to Files: On My iPhone/iPad > Styx2m > ${current.path.name}")
                availableSideloadingStores().forEach { store ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { openSideloadingStore(store) }
                    ) {
                        Text("Open ${store.label}")
                    }
                }
                if (UIDevice.currentDevice.userInterfaceIdiom != UIUserInterfaceIdiomPad) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { shareIpa(current.path) }
                    ) {
                        Text("Open Share Sheet")
                    }
                }
            }

            is IosUpdateState.Message -> Text(current.text, style = MaterialTheme.typography.labelMedium)
            IosUpdateState.Idle -> Unit
        }
    }
}

actual fun cleanupDownloadedUpdates() {
    runCatching {
        SYSTEMFILES.listOrNull(iosUpdateDirectory())
            .orEmpty()
            .filter { it.name.endsWith(".ipa", ignoreCase = true) }
            .forEach { SYSTEMFILES.delete(it, false) }
    }.onFailure {
        Log.e("AppUpdater", it) { "Failed to clean downloaded IPA updates." }
    }
}

private suspend fun downloadIpa(requestedVersion: String?, onProgress: (Int) -> Unit): IosUpdateState {
    val updateDir = iosUpdateDirectory()
    SYSTEMFILES.createDirectories(updateDir, false)
    val versionName = requestedVersion?.replace('/', '-') ?: "latest"
    val output = updateDir / "Styx2m-$versionName.ipa"
    if (SYSTEMFILES.exists(output)) {
        SYSTEMFILES.delete(output, false)
    }

    val versionPath = requestedVersion?.let { "/$it" }.orEmpty()
    val url = "${Endpoints.DOWNLOAD_BUILD_BASE.url()}/ios$versionPath?token=${login?.accessToken}"
    val result = downloadFileStream(url, output, onProgress)
    val size = SYSTEMFILES.metadataOrNull(output)?.size ?: 0L
    if (result !is DownloadResult.OK || size <= 0L) {
        runCatching { SYSTEMFILES.delete(output, false) }
        return IosUpdateState.Message("Failed to download IPA.")
    }
    return IosUpdateState.Downloaded(output)
}

@OptIn(ExperimentalForeignApi::class)
private fun iosUpdateDirectory(): Path {
    val documentsPath = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String
        ?: error("Could not resolve iOS Documents directory.")
    val styxDocuments = "$documentsPath/Styx2m"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = styxDocuments,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )
    return styxDocuments.toPath()
}

private fun shareIpa(path: Path) {
    val url = NSURL.fileURLWithPath(path.toString())
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController
    if (root == null) {
        Log.e("AppUpdater") { "Could not find a root view controller for the IPA share sheet." }
        return
    }
    val controller = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null
    )
    root.presentViewController(controller, animated = true, completion = null)
}

private fun availableSideloadingStores(): List<SideloadingStore> {
    return SideloadingStore.entries.filter { store ->
        val url = NSURL.URLWithString("${store.scheme}://") ?: return@filter false
        UIApplication.sharedApplication.canOpenURL(url)
    }
}

private fun openSideloadingStore(store: SideloadingStore) {
    val url = NSURL.URLWithString("${store.scheme}://")
    if (url == null) {
        Log.e("AppUpdater") { "Could not build URL for ${store.label}." }
        return
    }
    UIApplication.sharedApplication.openURL(
        url = url,
        options = emptyMap<Any?, Any>(),
        completionHandler = { success ->
            if (!success) {
                Log.e("AppUpdater") { "Failed to open ${store.label}." }
            }
        }
    )
}

private enum class SideloadingStore(val label: String, val scheme: String) {
    AltStore("AltStore", "altstore"),
    SideStore("SideStore", "sidestore")
}

private sealed interface IosUpdateState {
    data object Idle : IosUpdateState
    data object Downloading : IosUpdateState
    data class Downloaded(val path: Path) : IosUpdateState
    data class Message(val text: String) : IosUpdateState
}
