package moe.styx.styx2m.views.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import moe.styx.common.compose.components.misc.Toggles
import moe.styx.common.compose.settings

private val portraitCardChoices = listOf("3", "4", "5")
private val landScapeCardChoices = listOf("Adaptive", "7", "8", "9", "10")

@Composable
fun MetadataSettings() {
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