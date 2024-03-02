package moe.styx.styx2m.views

import Styx_m.composeApp.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.views.tabs.Tabs

class MainOverview : Screen {

    override val key: ScreenKey
        get() = "MainOverview"

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val sizes = LocalLayoutSize.current
        val useRail = sizes.isWide
        val defaultTab = if (settings["favs-startup", false]) Tabs.favsTab else Tabs.seriesTab
        settings["episode-list-index"] = 0
        TabNavigator(defaultTab) {
            MainScaffold(Modifier.fillMaxSize(),
                title = "${BuildConfig.APP_NAME} — Beta", addPopButton = false, actions = {
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

    @Composable
    private fun RowScope.TabNavItem(tab: Tab) {
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
    private fun RailNavItem(tab: Tab) {
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
}