package moe.styx.styx2m.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.misc.MpvCheckbox
import moe.styx.common.compose.components.misc.SettingsCheckbox
import moe.styx.common.compose.components.misc.StringChoices
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.*
import moe.styx.styx2m.misc.save

class SettingsView : Screen {
    private val portraitCardChoices = listOf("3", "4", "5")
    private val landScapeCardChoices = listOf("Adaptive", "7", "8", "9", "10")

    @Composable
    override fun Content() {
        MainScaffold(title = "Settings") {
            val scrollState = rememberScrollState()
            Column(Modifier.padding(8.dp).verticalScroll(scrollState, true)) {
                Text("Layout Options", modifier = Modifier.padding(5.dp), style = MaterialTheme.typography.headlineSmall)
                SettingsCheckbox("Show Names on cards", "display-names", false)
                SettingsCheckbox("Show episode summaries", "display-ep-synopsis", false)
                SettingsCheckbox("Prefer german titles and summaries", "prefer-german-metadata", false)
                HorizontalDivider(Modifier.padding(5.dp), thickness = 2.dp)
                SettingsCheckbox("Use list for shows", "shows-list", false)
                SettingsCheckbox("Use list for movies", "movies-list", false)
                SettingsCheckbox("Sort episodes ascendingly", "episode-asc", false)
                HorizontalDivider(Modifier.padding(5.dp), thickness = 2.dp)
                Column {
                    var porNumCards by remember { mutableStateOf(settings["portrait-cards", "3"]) }
                    StringChoices("Number of cards to show in portrait", portraitCardChoices, value = porNumCards) {
                        settings["portrait-cards"] = it
                        porNumCards = it
                        it
                    }
                    var landNumCards by remember { mutableStateOf(settings["landscape-cards", "7"]) }
                    StringChoices("Number of cards to show in landscape", landScapeCardChoices, value = landNumCards) {
                        settings["landscape-cards"] = it
                        landNumCards = it
                        it
                    }
                }
                HorizontalDivider(Modifier.padding(5.dp), thickness = 2.dp)
                Text("Mpv (Player) Options", modifier = Modifier.padding(5.dp), style = MaterialTheme.typography.headlineSmall)
                MpvSettings()
            }
        }
    }

    @Composable
    fun MpvSettings() {
        var preferences by remember { mutableStateOf(MpvPreferences.getOrDefault()) }
        Text("Performance / Quality", Modifier.padding(6.dp, 3.dp), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.padding(6.dp)) {
            MpvCheckbox(
                "Deband",
                preferences.deband,
                MpvDesc.deband
            ) { preferences = preferences.copy(deband = it).save() }
            StringChoices("Deband Iterations", debandIterationsChoices, "Higher = better (& slower)", preferences.debandIterations) {
                preferences = preferences.copy(debandIterations = it).save()
                it
            }

            StringChoices("Profile", profileChoices, MpvDesc.profileDescription, preferences.profile) {
                preferences = preferences.copy(profile = it).save()
                it
            }

            MpvCheckbox(
                "Hardware Decoding",
                preferences.hwDecoding,
                MpvDesc.hwDecoding
            ) { preferences = preferences.copy(hwDecoding = it).save() }
            MpvCheckbox(
                "Alternative Hardware Decoding",
                preferences.alternativeHwDecode,
                "If the other doesn't work properly but you want to try regardless."
            ) { preferences = preferences.copy(alternativeHwDecode = it).save() }

            StringChoices("GPU-API", gpuApiChoices, MpvDesc.gpuAPI, preferences.gpuAPI) {
                preferences = preferences.copy(gpuAPI = it).save()
                it
            }
            StringChoices("Video Output Driver", videoOutputDriverChoices, MpvDesc.outputDriver, preferences.videoOutputDriver) {
                preferences = preferences.copy(videoOutputDriver = it).save()
                it
            }
            MpvCheckbox(
                "Force 10bit Dithering",
                preferences.dither10bit,
                MpvDesc.dither10bit
            ) { preferences = preferences.copy(dither10bit = it).save() }
        }
        HorizontalDivider(Modifier.padding(5.dp), thickness = 2.dp)
        Text("Language Preferences", style = MaterialTheme.typography.titleLarge)
        Column(Modifier.padding(6.dp)) {
            Text(
                "If nothing here is checked, it will default to English sub",
                Modifier.padding(10.dp, 4.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            MpvCheckbox(
                "Prefer German subtitles",
                preferences.preferGerman
            ) { preferences = preferences.copy(preferGerman = it).save() }
            MpvCheckbox(
                "Prefer English dub",
                preferences.preferEnDub
            ) { preferences = preferences.copy(preferEnDub = it).save() }
            MpvCheckbox(
                "Prefer German dub",
                preferences.preferDeDub
            ) { preferences = preferences.copy(preferDeDub = it).save() }
        }
    }
}