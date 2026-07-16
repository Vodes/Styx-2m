package moe.styx.styx2m.views.misc

import Styx2m.styx2m.BuildConfig
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.navigation.Screen
import moe.styx.common.compose.navigation.ScreenKey
import moe.styx.common.compose.utils.openURI
import moe.styx.styx2m.views.settings.AppUpdateControls

class OutdatedView(private val requestedVersion: String? = null) : Screen {
    override val key: ScreenKey
        get() = requestedVersion?.let { "outdated-$it" } ?: "outdated"

    @Composable
    override fun Content() {
        MainScaffold(title = "Outdated", addPopButton = requestedVersion != null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (requestedVersion == null) "This version of Styx is outdated." else "Download $requestedVersion",
                    Modifier.padding(10.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.weight(0.3f))
                AppUpdateControls(requestedVersion, Modifier.weight(1f))
                Button(
                    {
                        openURI("${BuildConfig.SITE_URL}/user")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text("Open ${BuildConfig.SITE}")
                }
            }
        }
    }
}
