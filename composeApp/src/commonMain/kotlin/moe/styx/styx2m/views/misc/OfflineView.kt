package moe.styx.styx2m.views.misc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.ServerStatus

class OfflineView : Screen {

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Offline-Mode", style = MaterialTheme.typography.displaySmall)
            Text(ServerStatus.getLastKnownText(), style = MaterialTheme.typography.headlineSmall)
            Text("Feel free to keep using Styx with the data you have from your last use.", Modifier.padding(0.dp, 15.dp).weight(1f))

            Button({ nav.replaceAll(LoadingView()) }, Modifier.padding(10.dp)) {
                Text("OK")
            }
        }
    }
}