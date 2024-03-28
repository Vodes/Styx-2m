package moe.styx.styx2m.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import moe.styx.common.compose.components.buttons.ExpandIconButton
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.misc.Toggles.settingsContainer
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.views.settings.AppSettings
import moe.styx.styx2m.views.settings.MainSettings
import moe.styx.styx2m.views.settings.MpvSettings

class SettingsView : Screen {

    @Composable
    override fun Content() {
        val sizes = LocalLayoutSize.current
        if (sizes.isWide) {
            MainScaffold(title = "Settings") {
                var isLayoutExpanded by remember { mutableStateOf(true) }
                var isAppExpanded by remember { mutableStateOf(false) }
                Row {
                    Column(Modifier.padding(8.dp).weight(1f).verticalScroll(rememberScrollState(), true)) {
                        ExpandableSettings(
                            "Layout Options",
                            isLayoutExpanded,
                            {
                                isLayoutExpanded = !isLayoutExpanded
                                if (isLayoutExpanded)
                                    isAppExpanded = false
                            }) {
                            MainSettings()
                        }

                        ExpandableSettings(
                            "App Settings",
                            isAppExpanded,
                            {
                                isAppExpanded = !isAppExpanded;
                                if (isAppExpanded)
                                    isLayoutExpanded = false
                            }) {
                            AppSettings()
                        }
                    }
                    VerticalDivider(Modifier.fillMaxHeight().padding(3.dp, 8.dp), thickness = 2.dp)
                    Column(Modifier.padding(8.dp).weight(1f).verticalScroll(rememberScrollState(), true)) {
                        MpvSettings()
                    }
                }
            }
        } else {
            MainScaffold(title = "Settings") {
                val scrollState = rememberScrollState()
                var isLayoutExpanded by remember { mutableStateOf(true) }
                var isPlayerExpanded by remember { mutableStateOf(false) }
                var isAppExpanded by remember { mutableStateOf(false) }
                Column(Modifier.padding(8.dp).verticalScroll(scrollState, true)) {
                    ExpandableSettings(
                        "Layout Options",
                        isLayoutExpanded,
                        {
                            isLayoutExpanded = !isLayoutExpanded
                            if (isLayoutExpanded) {
                                isPlayerExpanded = false
                                isAppExpanded = false
                            }
                        }) {
                        MainSettings()
                    }

                    ExpandableSettings(
                        "App Settings",
                        isAppExpanded,
                        {
                            isAppExpanded = !isAppExpanded;
                            if (isAppExpanded) {
                                isLayoutExpanded = false
                                isPlayerExpanded = false
                            }
                        }) {
                        AppSettings()
                    }


                    ExpandableSettings(
                        "Player Options",
                        isPlayerExpanded,
                        {
                            isPlayerExpanded = !isPlayerExpanded;
                            if (isPlayerExpanded) {
                                isLayoutExpanded = false
                                isAppExpanded = false
                            }
                        },
                        false
                    ) {
                        MpvSettings()
                    }
                }
            }
        }
    }

    @Composable
    fun ExpandableSettings(
        title: String,
        isExpanded: Boolean,
        onExpandClick: () -> Unit,
        withContainer: Boolean = true,
        content: (@Composable ColumnScope.() -> Unit)
    ) {
        ElevatedCard(onExpandClick, Modifier.padding(5.dp)) {
            Row(Modifier.fillMaxWidth().padding(3.dp)) {
                Text(title, modifier = Modifier.padding(5.dp).weight(1f), style = MaterialTheme.typography.headlineSmall)
                ExpandIconButton(isExpanded = isExpanded, onClick = onExpandClick)
            }
            AnimatedVisibility(isExpanded) {
                if (withContainer) {
                    Column(Modifier.settingsContainer()) {
                        content()
                    }
                } else {
                    Column(Modifier.fillMaxWidth()) {
                        content()
                    }
                }
            }
        }
    }
}