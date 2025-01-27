package moe.styx.styx2m.views.misc

import Styx2m.styx2m.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import moe.styx.common.Platform
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.http.Endpoints
import moe.styx.common.compose.http.login
import moe.styx.styx2m.openUrl

class OutdatedView(private val requestedVersion: String? = null) : Screen {

    @Composable
    override fun Content() {
        MainScaffold(title = "Outdated", addPopButton = requestedVersion != null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (requestedVersion == null) "This version of Styx is outdated." else "Download $requestedVersion",
                    Modifier.padding(10.dp).weight(1f),
                    style = MaterialTheme.typography.headlineMedium
                )
                if (Platform.current == Platform.ANDROID)
                    AndroidDownloadButtons()
                Button(
                    {
                        openUrl("${BuildConfig.SITE_URL}/user")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Open ${BuildConfig.SITE}")
                }
            }
        }
    }

    @Composable
    fun ColumnScope.AndroidDownloadButtons() {
        Column(Modifier.weight(1f)) {
            Text("This has to match what you installed first for a seamless update.", Modifier.padding(8.dp))
            Row {
                Button({
                    openUrl(Endpoints.DOWNLOAD_BUILD_BASE.url() + "/android-arm64" + (if (requestedVersion != null) "/$requestedVersion" else "") + "?token=${login?.accessToken}")
                }, modifier = Modifier.padding(12.dp)) {
                    Text("ARM-64 APK")
                }

                Button({
                    openUrl(Endpoints.DOWNLOAD_BUILD_BASE.url() + "/android-universal" + (if (requestedVersion != null) "/$requestedVersion" else "") + "?token=${login?.accessToken}")
                }, modifier = Modifier.padding(12.dp)) {
                    Text("Universal APK")
                }
            }
        }
    }
}