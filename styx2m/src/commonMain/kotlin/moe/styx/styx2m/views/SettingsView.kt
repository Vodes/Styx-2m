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
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import moe.styx.common.compose.components.buttons.ExpandIconButton
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.misc.Toggles
import moe.styx.common.compose.components.misc.Toggles.settingsContainer
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.*
import moe.styx.styx2m.misc.LocalLayoutSize
import moe.styx.styx2m.misc.save

class SettingsView : Screen {
    private val portraitCardChoices = listOf("3", "4", "5")
    private val landScapeCardChoices = listOf("Adaptive", "7", "8", "9", "10")

    @Composable
    override fun Content() {
        val sizes = LocalLayoutSize.current
        if (sizes.isWide) {
            MainScaffold(title = "Settings") {
                Row {
                    Column(Modifier.padding(8.dp).weight(1f).verticalScroll(rememberScrollState(), true)) {
                        MainSettings()
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
                Column(Modifier.padding(8.dp).verticalScroll(scrollState, true)) {
                    MainSettings()
                    var isExpanded by remember { mutableStateOf(false) }
                    ElevatedCard(modifier = Modifier.padding(5.dp), onClick = { isExpanded = !isExpanded }) {
                        Row(Modifier.fillMaxWidth().padding(3.dp)) {
                            Text("Player Settings", modifier = Modifier.padding(5.dp).weight(1f), style = MaterialTheme.typography.headlineSmall)
                            ExpandIconButton(isExpanded = isExpanded) {
                                isExpanded = !isExpanded
                            }
                        }
                        AnimatedVisibility(isExpanded) {
                            MpvSettings()
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun MainSettings() {
        Column(Modifier.settingsContainer()) {
            Text("Layout Options", modifier = Modifier.padding(10.dp, 7.dp), style = MaterialTheme.typography.titleLarge)
            Toggles.ContainerSwitch("Show names on cards", value = settings["display-names", false]) { settings["display-names"] = it }
            Toggles.ContainerSwitch(
                "Show episode summaries",
                value = settings["display-ep-synopsis", false]
            ) { settings["display-ep-synopsis"] = it }
            Toggles.ContainerSwitch(
                "Prefer german titles and summaries",
                value = settings["prefer-german-metadata", false]
            ) { settings["prefer-german-metadata"] = it }
            Row(Modifier.fillMaxWidth()) {
                Toggles.ContainerSwitch(
                    "Use list for shows",
                    modifier = Modifier.weight(1f),
                    value = settings["shows-list", false],
                    paddingValues = Toggles.rowStartPadding
                ) { settings["shows-list"] = it }
                Toggles.ContainerSwitch(
                    "Use list for movies",
                    modifier = Modifier.weight(1f),
                    value = settings["movies-list", false],
                    paddingValues = Toggles.rowEndPadding
                ) { settings["movies-list"] = it }
            }
            Toggles.ContainerSwitch(
                "Sort episodes ascendingly",
                value = settings["episode-asc", false],
                paddingValues = Toggles.colEndPadding
            ) { settings["episode-asc"] = it }

            Toggles.ContainerRadioSelect(
                "Number of cards to show in portrait",
                value = settings["portrait-cards", "3"],
                choices = portraitCardChoices
            ) {
                settings["portrait-cards"] = it
            }
            Toggles.ContainerRadioSelect(
                "Number of cards to show in landscape",
                value = settings["landscape-cards", "7"],
                choices = landScapeCardChoices
            ) {
                settings["landscape-cards"] = it
            }
            Spacer(Modifier.height(3.dp))
        }
    }

    @Composable
    fun MpvSettings() {
        var preferences by remember { mutableStateOf(MpvPreferences.getOrDefault()) }
        println(preferences)
        Column {
            Column(Modifier.settingsContainer()) {
                Text("Language Preferences", Modifier.padding(10.dp, 7.dp), style = MaterialTheme.typography.titleLarge)
                Text(
                    "If nothing is selected here, it defaults to english subtitles.",
                    Modifier.padding(10.dp, 5.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Toggles.ContainerSwitch(
                    "Prefer German subtitles", value = preferences.preferGerman,
                ) { preferences = preferences.copy(preferGerman = it).save() }
                Toggles.ContainerSwitch(
                    "Prefer German dub", value = preferences.preferDeDub,
                ) { preferences = preferences.copy(preferDeDub = it).save() }
                Toggles.ContainerSwitch(
                    "Prefer English dub", value = preferences.preferEnDub,
                ) { preferences = preferences.copy(preferEnDub = it).save() }
                Spacer(Modifier.height(3.dp))
            }

            Column(Modifier.settingsContainer()) {
                Text("Performance / Quality", Modifier.padding(10.dp, 7.dp), style = MaterialTheme.typography.titleLarge)
                Toggles.ContainerSwitch(
                    "Deband",
                    MpvDesc.deband,
                    value = preferences.deband,
                ) { preferences = preferences.copy(deband = it).save() }
                Toggles.ContainerRadioSelect(
                    "Deband Iterations",
                    "Higher = better (& slower)",
                    value = preferences.debandIterations,
                    choices = debandIterationsChoices,
                ) { preferences = preferences.copy(debandIterations = it).save() }

                Toggles.ContainerRadioSelect(
                    "Profile", MpvDesc.profileDescription, value = preferences.profile, choices = profileChoices,
                ) { preferences = preferences.copy(profile = it).save() }

                Toggles.ContainerSwitch(
                    "Hardware Decoding",
                    MpvDesc.hwDecoding,
                    value = preferences.hwDecoding,
                ) { preferences = preferences.copy(hwDecoding = it).save() }
                Toggles.ContainerSwitch(
                    "Alternative Hardware Decoding",
                    "If the other doesn't work properly but you want to try regardless.",
                    value = preferences.alternativeHwDecode,
                ) { preferences = preferences.copy(alternativeHwDecode = it).save() }


                Toggles.ContainerRadioSelect(
                    "GPU-API", MpvDesc.gpuAPI, value = preferences.gpuAPI, choices = gpuApiChoices,
                ) { preferences = preferences.copy(gpuAPI = it).save() }
                Toggles.ContainerRadioSelect(
                    "Video Output Driver", MpvDesc.outputDriver, value = preferences.videoOutputDriver, choices = videoOutputDriverChoices,
                ) { preferences = preferences.copy(videoOutputDriver = it).save() }
                Toggles.ContainerSwitch(
                    "Force 10bit Dithering", MpvDesc.dither10bit, value = preferences.dither10bit,
                ) { preferences = preferences.copy(dither10bit = it).save() }
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}