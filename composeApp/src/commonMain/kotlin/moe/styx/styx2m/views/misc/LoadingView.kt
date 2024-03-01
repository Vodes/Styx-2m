package moe.styx.styx2m.views.misc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.files.Storage
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.util.launchGlobal
import moe.styx.styx2m.views.MainOverview

class LoadingView : Screen {
    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val coroutineScope = rememberCoroutineScope()

        coroutineScope.launch {
            delay(1000)
            launchGlobal {
                Storage.stores.mediaStore.get()
                Storage.stores.favouriteStore.get()
            }.join()
            delay(1000)
            nav.replaceAll(MainOverview())
        }

        MainScaffold(title = "Loading", addPopButton = false) {
            val progress by Storage.loadingProgress.collectAsState()
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

