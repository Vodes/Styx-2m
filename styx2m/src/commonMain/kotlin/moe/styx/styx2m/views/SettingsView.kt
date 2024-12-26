package moe.styx.styx2m.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.misc.ExpandableSettings
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.views.settings.AppSettings
import moe.styx.styx2m.views.settings.AppearanceSettings
import moe.styx.styx2m.views.settings.MetadataSettings
import moe.styx.styx2m.views.settings.MpvSettings

class SettingsView : Screen {

    @Composable
    override fun Content() {
        val sizes = LocalLayoutSize.current

        val vm = if (sizes.isWide)
            rememberScreenModel("wide-settings-vm") { SettingsViewModel() }
        else
            rememberScreenModel("settings-vm") { SettingsViewModel() }

        MainScaffold(title = "Settings") {
            Row {
                Column(Modifier.padding(8.dp).weight(1f).verticalScroll(rememberScrollState(), true)) {
                    ExpandableSettings("Appearance Settings", vm.appearanceExpanded, { vm.appearanceExpanded = !vm.appearanceExpanded }) {
                        AppearanceSettings()
                    }
                    ExpandableSettings("Metadata Settings", vm.metadataExpanded, { vm.metadataExpanded = !vm.metadataExpanded }) {
                        MetadataSettings()
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

    private val allBackingMutables = arrayOf(_metadataExpanded, _appExpanded, _playerExpanded, _appearanceExpanded)
}