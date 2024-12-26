package moe.styx.styx2m.views.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import moe.styx.common.compose.components.misc.Toggles
import moe.styx.common.compose.components.misc.Toggles.settingsContainer
import moe.styx.common.compose.settings
import moe.styx.common.compose.utils.*
import moe.styx.styx2m.misc.save


val hwDecoding = """
        Use your GPU to decode the video. Both faster and more efficient.
        Leave on if at all possible and not causing issues.
        Copy is the same as yes but with copying the frame back to ram first.
        This may (will) work more reliably but will use more ram.
    """.trimIndent()

@Composable
fun MpvSettings() {
    var preferences by remember { mutableStateOf(MpvPreferences.getOrDefault()) }
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

            Toggles.ContainerRadioSelect(
                "Hardware Decoding", hwDecoding, value = settings["hwdec", "copy"], choices = listOf("no", "yes", "copy"),
            ) { settings["hwdec"] = it }

            Toggles.ContainerRadioSelect(
                "GPU-API", MpvDesc.gpuAPI, value = preferences.gpuAPI, choices = gpuApiChoices,
            ) { preferences = preferences.copy(gpuAPI = it).save() }
            Toggles.ContainerRadioSelect(
                "Video Output Driver", MpvDesc.outputDriver, value = preferences.videoOutputDriver, choices = videoOutputDriverChoices,
            ) { preferences = preferences.copy(videoOutputDriver = it).save() }
            Toggles.ContainerSwitch(
                "Force 10bit Dithering", MpvDesc.dither10bit, value = preferences.dither10bit,
            ) { preferences = preferences.copy(dither10bit = it).save() }
            Toggles.ContainerSwitch(
                "Blend Subtitles", MpvDesc.blendSubs, value = preferences.blendSubs,
            ) { preferences = preferences.copy(blendSubs = it).save() }
            Spacer(Modifier.height(3.dp))
        }
    }
}