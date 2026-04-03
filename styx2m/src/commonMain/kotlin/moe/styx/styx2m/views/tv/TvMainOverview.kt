package moe.styx.styx2m.views.tv

import Styx2m.styx2m.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.dokar.sonner.TextToastAction
import com.dokar.sonner.Toast
import com.dokar.sonner.ToasterDefaults
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.misc.OnlineUsersIcon
import moe.styx.common.compose.http.login
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.LocalToaster
import moe.styx.common.compose.utils.ServerStatus
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.styx2m.misc.pushMediaView
import moe.styx.styx2m.views.AboutView
import moe.styx.styx2m.views.MobileOverviewModel
import moe.styx.styx2m.views.SideNavRail
import moe.styx.styx2m.views.misc.LoginView
import moe.styx.styx2m.views.misc.OutdatedView
import moe.styx.styx2m.views.tabs.Tabs
import androidx.compose.material3.surfaceColorAtElevation

class TvAnimeOverview : Screen {

    @Composable
    override fun Content() {
        val toaster = LocalToaster.current
        val overviewSm = rememberScreenModel { MobileOverviewModel() }
        val nav = LocalGlobalNavigator.current

        if (overviewSm.isOutdated == true) {
            nav.replaceAll(OutdatedView())
        }

        if (overviewSm.isLoggedIn == false && ServerStatus.lastKnown == ServerStatus.UNAUTHORIZED) {
            nav.replaceAll(LoginView())
        }

        LaunchedEffect(overviewSm.availablePreRelease) {
            val ver = overviewSm.availablePreRelease
            if (!ver.isNullOrBlank()) {
                toaster.show(
                    Toast(
                        "New Pre-Release version available: $ver",
                        action = TextToastAction("Download") {
                            nav.push(OutdatedView(ver))
                        },
                        duration = ToasterDefaults.DurationLong
                    )
                )
                overviewSm.availablePreRelease = null
            }
        }

        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        val isLoading by sm.isLoadingStateFlow.collectAsState()
        val defaultTab = if (settings["favs-startup", true]) Tabs.favsTab else Tabs.seriesTab
        settings["episode-list-index"] = 0

        TabNavigator(defaultTab) {
            MainScaffold(
                modifier = Modifier.fillMaxSize(),
                title = BuildConfig.APP_NAME,
                addPopButton = false,
                addAnimatedTitleBackground = true,
                titleClickable = { nav.push(AboutView()) },
                actions = {
                    if (isLoading) {
                        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                trackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(18.dp),
                                gapSize = 0.dp,
                                modifier = Modifier.requiredWidthIn(26.dp, 60.dp)
                            )
                        }
                    }
                    if (overviewSm.isOffline == true) {
                        IconButtonWithTooltip(Icons.Filled.CloudOff, ServerStatus.getLastKnownText()) {}
                    }
                    if (overviewSm.isLoggedIn == false) {
                        IconButtonWithTooltip(Icons.Filled.NoAccounts, "You are not logged in!\nClick to retry.") {
                            overviewSm.screenModelScope.launch {
                                val loginJob = overviewSm.runLoginAndChecks()
                                loginJob.join()
                                if (login != null) {
                                    sm.updateData(updateStores = true)
                                }
                            }
                        }
                    }
                    OnlineUsersIcon { nav.pushMediaView(it) }
                }
            ) {
                Row(
                    Modifier.fillMaxSize().padding(0.dp, 0.dp, 10.dp, 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SideNavRail(nav, true)
                    CurrentTab()
                }
            }
        }
    }
}
