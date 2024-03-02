package moe.styx.styx2m.views.misc

import Styx_m.styx_m.BuildConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.delay
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.ServerStatus
import moe.styx.styx2m.misc.isUpToDate
import moe.styx.styx2m.openUrl
import moe.styx.styx2m.views.MainOverview

class LoadingView : Screen {

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val scope = rememberCoroutineScope()
        val progress by Storage.loadingProgress.collectAsState("", scope.coroutineContext)

        if (ServerStatus.lastKnown != ServerStatus.UNKNOWN && !isUpToDate()) {
            OutdatedVersion()
            return
        }

        LaunchedEffect(Unit) {
            delay(1000)
            Storage.loadData()
            delay(1000)
            nav.replaceAll(MainOverview())
        }

        MainScaffold(title = "Loading", addPopButton = false) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.padding(10.dp).align(Alignment.TopCenter)) {
                    Text(
                        text = progress,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(0.dp, 25.dp)
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(0.dp, 15.dp).fillMaxSize(.4F)
                    )
                }
            }
        }
    }
}

@Composable
fun OutdatedVersion() {
    MainScaffold(title = "Outdated", addPopButton = false) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "This version of Styx is outdated.\nPlease update.",
                Modifier.padding(10.dp).weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = {
                openUrl(BuildConfig.SITE_URL)
            }, Modifier.padding(10.dp)) {
                Text("Open ${BuildConfig.SITE}")
            }
        }
    }
}

