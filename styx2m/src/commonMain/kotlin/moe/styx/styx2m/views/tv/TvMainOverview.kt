package moe.styx.styx2m.views.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.ServerStatus
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.styx2m.views.MobileOverviewModel
import moe.styx.styx2m.views.SideNavRail
import moe.styx.styx2m.views.misc.LoginView
import moe.styx.styx2m.views.misc.OutdatedView
import moe.styx.styx2m.views.tabs.Tabs.favsTab

class TvAnimeOverview : Screen {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val overviewSm = rememberScreenModel { MobileOverviewModel() }
        val nav = LocalGlobalNavigator.current

        if (overviewSm.isOutdated == true) {
            nav.replaceAll(OutdatedView())
        }

        if (overviewSm.isLoggedIn == false && ServerStatus.lastKnown == ServerStatus.UNAUTHORIZED) {
            nav.replaceAll(LoginView())
        }

        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        TabNavigator(favsTab) {
            Row {
                SideNavRail(nav, true)
                CurrentTab()
            }
        }
    }
}