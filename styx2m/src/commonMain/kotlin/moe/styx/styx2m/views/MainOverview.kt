package moe.styx.styx2m.views

import Styx_m.styx_m.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import moe.styx.styx2m.views.tabs.MediaTab
import moe.styx.styx2m.views.tabs.Tabs

class MainOverview : Screen {

    override val key: ScreenKey
        get() = "MainOverview"

    @OptIn(ExperimentalMaterial3Api::class)
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

        val sizes = LocalLayoutSize.current
        val useRail = sizes.isWide
        val defaultTab = if (settings["favs-startup", false]) Tabs.favsTab else Tabs.seriesTab
        settings["episode-list-index"] = 0
        TabNavigator(defaultTab) {
            val tabNav = LocalTabNavigator.current
            MainScaffold(
                Modifier.fillMaxSize(),
                topAppBarExpandedHeight = if (useRail) 96.dp else TopAppBarDefaults.TopAppBarExpandedHeight,
                titleContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(BuildConfig.APP_NAME)
                        if (useRail) {
                            key(tabNav.current) {
                                (tabNav.current as? MediaTab)?.HeaderSearchBar(Modifier.heightIn(48.dp).weight(1f).padding(15.dp, 0.dp))
                            }
                        }
                    }
                }, addPopButton = false, actions = {
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
        NavigationRail(Modifier.fillMaxHeight().padding(0.dp, 0.dp, 5.dp, 0.dp)) {
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
        label = { Text(tab.options.title) },
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