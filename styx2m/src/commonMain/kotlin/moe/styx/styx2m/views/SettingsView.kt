package moe.styx.styx2m.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import moe.styx.common.compose.components.buttons.IconButtonWithTooltip
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.misc.ExpandableSettings
import moe.styx.common.compose.components.settings.MetadataSettings
import moe.styx.common.compose.components.settings.TrackingSettings
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.LocalIsTv
import moe.styx.common.compose.utils.LocalLayoutSize
import moe.styx.styx2m.views.settings.AppSettings
import moe.styx.styx2m.views.settings.AppearanceSettings
import moe.styx.styx2m.views.settings.MpvSettings

class SettingsView : Screen {

    @Composable
    override fun Content() {
        val sizes = LocalLayoutSize.current
        val nav = LocalGlobalNavigator.current
        val isTv = LocalIsTv.current
        val vm = if (!isTv) {
            if (sizes.isWide)
                rememberScreenModel("wide-settings-vm") { SettingsViewModel() }
            else
                rememberScreenModel("settings-vm") { SettingsViewModel() }
        } else null

        MainScaffold(title = "Settings", actions = {
            IconButtonWithTooltip(Icons.Default.Info, "View info about this app") {
                nav.push(AboutView())
            }
        }) {
            if (isTv) {
                TvSettingsContent()
                return@MainScaffold
            }

            Row {
                Column(Modifier.padding(8.dp).weight(1f).verticalScroll(rememberScrollState(), true)) {
                    ExpandableSettings("Appearance Settings", vm!!.appearanceExpanded, { vm.appearanceExpanded = !vm.appearanceExpanded }) {
                        AppearanceSettings()
                    }
                    ExpandableSettings("Metadata Settings", vm.metadataExpanded, { vm.metadataExpanded = !vm.metadataExpanded }) {
                        MetadataSettings()
                    }
                    ExpandableSettings("Tracking Settings", vm.trackingExpanded, { vm.trackingExpanded = !vm.trackingExpanded }) {
                        TrackingSettings()
                    }
                    ExpandableSettings("App Settings", vm.appExpanded, { vm.appExpanded = !vm.appExpanded; }) {
                        AppSettings()
                    }
                    if (!sizes.isWide) {
                        ExpandableSettings("Player Settings", vm.playerExpanded, { vm.playerExpanded = !vm.playerExpanded; }, false) {
                            MpvSettings()
                        }
                    }
                }
                if (sizes.isWide) {
                    VerticalDivider(Modifier.fillMaxHeight().padding(3.dp, 8.dp), thickness = 2.dp)
                    Column(Modifier.padding(8.dp).weight(1f).verticalScroll(rememberScrollState(), true)) {
                        MpvSettings()
                    }
                }
            }
        }
    }
}

@Composable
private fun TvSettingsContent() {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TvSettingsSection("Appearance Settings") { AppearanceSettings() } }
        item { TvSettingsSection("Metadata Settings") { MetadataSettings() } }
        item { TvSettingsSection("Tracking Settings") { TrackingSettings() } }
        item { TvSettingsSection("App Settings") { AppSettings() } }
        item { TvSettingsSection("Player Settings") { MpvSettings() } }
    }
}

@Composable
private fun TvSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

class SettingsViewModel : ScreenModel {
    private var _appearanceExpanded = mutableStateOf(true)
    var appearanceExpanded: Boolean
        get() = _appearanceExpanded.value
        set(value) {
            _appearanceExpanded.value = value
            allBackingMutables.forEach { if (it != _appearanceExpanded) it.value = false }
        }

    private var _metadataExpanded = mutableStateOf(false)
    var metadataExpanded: Boolean
        get() = _metadataExpanded.value
        set(value) {
            _metadataExpanded.value = value
            allBackingMutables.forEach { if (it != _metadataExpanded) it.value = false }
        }

    private var _appExpanded = mutableStateOf(false)
    var appExpanded: Boolean
        get() = _appExpanded.value
        set(value) {
            _appExpanded.value = value
            allBackingMutables.forEach { if (it != _appExpanded) it.value = false }
        }

    private var _playerExpanded = mutableStateOf(false)
    var playerExpanded: Boolean
        get() = _playerExpanded.value
        set(value) {
            _playerExpanded.value = value
            allBackingMutables.forEach { if (it != _playerExpanded) it.value = false }
        }

    private var _trackingExpanded = mutableStateOf(false)
    var trackingExpanded: Boolean
        get() = _trackingExpanded.value
        set(value) {
            _trackingExpanded.value = value
            allBackingMutables.forEach { if (it != _trackingExpanded) it.value = false }
        }


    private val allBackingMutables = arrayOf(_metadataExpanded, _appExpanded, _playerExpanded, _appearanceExpanded, _trackingExpanded)
}
