package moe.styx.styx2m.views

import Styx2m.styx2m.BuildConfig
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import moe.styx.common.compose.components.about.AboutViewComponent
import moe.styx.common.compose.components.layout.MainScaffold

class AboutView : Screen {

    @Composable
    override fun Content() {
        MainScaffold(title = "About") {
            AboutViewComponent(BuildConfig.APP_NAME)
        }
    }
}