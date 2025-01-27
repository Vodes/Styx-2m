package moe.styx.styx2m.views

import Styx2m.styx2m.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberNavigatorScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.dokar.sonner.TextToastAction
import com.dokar.sonner.Toast
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.launch
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.misc.OnlineUsersIcon
import moe.styx.common.compose.http.login
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.LocalToaster
import moe.styx.common.compose.utils.ServerStatus
import moe.styx.common.compose.viewmodels.MainDataViewModel
import moe.styx.common.compose.viewmodels.OverviewViewModel
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.misc.pushMediaView
import moe.styx.styx2m.views.misc.LoginView
import moe.styx.styx2m.views.misc.OutdatedView
import moe.styx.styx2m.views.tabs.Tabs

class MainOverview : Screen {

    override val key: ScreenKey
        get() = "MainOverview"

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
                        }
                    )
                )
                overviewSm.availablePreRelease = null
            }
        }

        val sm = nav.rememberNavigatorScreenModel("main-vm") { MainDataViewModel() }
        val isLoading by sm.isLoadingStateFlow.collectAsState()
        val loadingState by sm.loadingStateFlow.collectAsState()

        val sizes = LocalLayoutSize.current
        val useRail = sizes.isWide
        val defaultTab = if (settings["favs-startup", false]) Tabs.favsTab else Tabs.seriesTab
        settings["episode-list-index"] = 0
        TabNavigator(defaultTab) {
            MainScaffold(
                Modifier.fillMaxSize(),
                title = BuildConfig.APP_NAME, addPopButton = false, addAnimatedTitleBackground = useRail, actions = {
                    if (isLoading) {
                        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                trackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(18.dp),
                                gapSize = 0.dp,
                                modifier = Modifier.requiredWidthIn(20.dp, 40.dp)
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
                    if (!useRail)
                        IconButtonWithTooltip(Icons.Filled.Settings, "Settings") { nav.push(SettingsView()) }
                }, bottomBarContent = {
                    if (!useRail)
                        BottomNavBar()
                }
            ) {
                if (useRail) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
                        SideNavRail(nav, sizes.isLandScape)
                        CurrentTab()
                    }
                } else {
                    CurrentTab()
                }
            }
        }
    }

    @Composable
    private fun BottomNavBar() {
        NavigationBar(tonalElevation = 10.dp) {
            TabNavItem(Tabs.seriesTab)
            TabNavItem(Tabs.moviesTab)
            TabNavItem(Tabs.favsTab)
            TabNavItem(Tabs.scheduleTab)
        }
    }

    @Composable
    private fun SideNavRail(parentNav: Navigator, isLandscape: Boolean) {
        NavigationRail(
            Modifier.fillMaxHeight().padding(7.dp, 6.dp, 3.dp, 8.dp).shadow(2.dp, AppShapes.large)
                .clip(AppShapes.large),
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ) {
            RailNavItem(Tabs.seriesTab)
            RailNavItem(Tabs.moviesTab)
            RailNavItem(Tabs.favsTab)
            RailNavItem(Tabs.scheduleTab)
            if (isLandscape)
                Spacer(Modifier.weight(1f))
            else
                Spacer(Modifier.height(50.dp))
            NavigationRailItem(
                selected = false, onClick = { parentNav.push(SettingsView()) },
                icon = { Icon(Icons.Filled.Settings, "Settings") },
                label = { Text("Settings") }, alwaysShowLabel = true,
            )
        }
    }
}

@Composable
fun RowScope.TabNavItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationBarItem(
        selected = tabNavigator.current.key == tab.key,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(painter = tab.options.icon!!, contentDescription = tab.options.title) },
        label = { Text(tab.options.title) },
        alwaysShowLabel = false,
        colors = NavigationBarItemDefaults.colors(
            unselectedIconColor = MaterialTheme.colorScheme.onSurface,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun RailNavItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current

    NavigationRailItem(
        selected = tabNavigator.current.key == tab.key,
        onClick = { tabNavigator.current = tab },
        icon = { Icon(painter = tab.options.icon!!, contentDescription = tab.options.title) },
        label = { Text(tab.options.title, modifier = Modifier.padding(3.dp, 1.dp)) },
        alwaysShowLabel = true,
        colors = NavigationRailItemDefaults.colors(
            unselectedIconColor = MaterialTheme.colorScheme.onSurface,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary
        )
    )
}

class MobileOverviewModel : OverviewViewModel()