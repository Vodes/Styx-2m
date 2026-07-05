package moe.styx.styx2m.views.settings

import Styx2m.styx2m.BuildConfig
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.styx.common.compose.AppContextImpl.appConfig
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.http.Endpoints
import moe.styx.common.compose.http.login
import moe.styx.common.compose.utils.LocalIsTv
import moe.styx.common.http.DownloadResult
import moe.styx.common.http.downloadFileStream
import moe.styx.common.util.Log
import moe.styx.common.util.SYSTEMFILES
import okio.Path.Companion.toPath
import java.io.File

@Composable
actual fun AppUpdateControls(requestedVersion: String?, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isTv = LocalIsTv.current
    val variants = remember(isTv) {
        if (isTv) listOf(UpdateVariant.Universal, UpdateVariant.Arm64) else listOf(UpdateVariant.Arm64, UpdateVariant.Universal)
    }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    val progressFlow = remember { MutableStateFlow(0) }
    val progress by progressFlow.collectAsState()
    val isBusy = state is UpdateState.Downloading

    Column(modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Current version: ${BuildConfig.APP_VERSION}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Use the APK type that matches what is currently installed.",
            style = MaterialTheme.typography.labelMedium
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            variants.forEachIndexed { index, variant ->
                UpdateButton(
                    text = "${variant.label}${if (index == 0) " (Recommended)" else ""}",
                    isTv = isTv,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    scope.launch {
                        if (!context.canInstallPackages()) {
                            state = UpdateState.Message("Allow installs from Styx, then tap the update button again.")
                            context.openUnknownSourcesSettings()
                            return@launch
                        }
                        progressFlow.value = 0
                        state = UpdateState.Downloading(variant)
                        val result = withContext(Dispatchers.IO) {
                            downloadUpdateApk(context, variant, requestedVersion) { progress ->
                                progressFlow.value = progress
                            }
                        }
                        state = result
                    }
                }
            }
        }
        when (val current = state) {
            is UpdateState.Downloading -> {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Downloading ${current.variant.label}: $progress%")
            }
            is UpdateState.Message -> Text(current.text, style = MaterialTheme.typography.labelMedium)
            UpdateState.Idle -> Unit
        }
    }
}

@Composable
private fun UpdateButton(
    text: String,
    isTv: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary
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
    val tvModifier = if (isTv) {
        Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                if (isFocused) 3.dp else 1.dp,
                if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                AppShapes.large
            )
            .heightIn(min = 48.dp)
    } else {
        Modifier
    }

    Button(
        onClick = onClick,
        modifier = modifier.then(tvModifier),
        enabled = enabled,
        shape = AppShapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedContainerColor,
            contentColor = if (isTv && !isFocused) contentColor.copy(alpha = 0.92f) else contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, focusedElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Text(text, fontWeight = if (isTv && isFocused) FontWeight.SemiBold else FontWeight.Medium)
    }
}

private suspend fun downloadUpdateApk(
    context: Context,
    variant: UpdateVariant,
    requestedVersion: String?,
    onProgress: (Int) -> Unit
): UpdateState {
    val updateDir = "${appConfig().appCachePath}/updates".toPath()
    SYSTEMFILES.createDirectories(updateDir, false)
    val output = updateDir / "styx2m-update-${variant.id}.apk"
    if (SYSTEMFILES.exists(output)) {
        SYSTEMFILES.delete(output, false)
    }

    val versionPath = requestedVersion?.let { "/$it" }.orEmpty()
    val url = "${Endpoints.DOWNLOAD_BUILD_BASE.url()}/${variant.endpoint}$versionPath?token=${login?.accessToken}"
    val downloadResult = downloadFileStream(url, output, onProgress)
    val size = SYSTEMFILES.metadataOrNull(output)?.size ?: 0L
    if (downloadResult !is DownloadResult.OK || size <= 0L) {
        runCatching { SYSTEMFILES.delete(output, false) }
        return UpdateState.Message("Failed to download ${variant.label}.")
    }

    return context.installApk(File(output.toString()))
}

private fun Context.installApk(apk: File): UpdateState {
    if (!canInstallPackages()) {
        openUnknownSourcesSettings()
        return UpdateState.Message("Allow installs from Styx, then tap the update button again.")
    }
    return runCatching {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        UpdateState.Message("Installer opened.")
    }.onFailure {
        Log.e("AppUpdater", it) { "Failed to open APK installer." }
    }.getOrElse {
        UpdateState.Message("Failed to open the Android installer.")
    }
}

private fun Context.canInstallPackages(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()
}

private fun Context.openUnknownSourcesSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return
    }
    val intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:$packageName")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching {
        startActivity(intent)
    }.recoverCatching {
        if (it !is ActivityNotFoundException) throw it
        startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Log.e("AppUpdater", it) { "Failed to open unknown source settings." }
    }
}

private enum class UpdateVariant(val id: String, val label: String, val endpoint: String) {
    Arm64("arm64", "ARM-64 APK", "android-arm64"),
    Universal("universal", "Universal APK", "android-universal")
}

private sealed interface UpdateState {
    data object Idle : UpdateState
    data class Downloading(val variant: UpdateVariant) : UpdateState
    data class Message(val text: String) : UpdateState
}
