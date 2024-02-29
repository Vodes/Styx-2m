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
import com.moriatsushi.insetsx.safeAreaPadding
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import moe.styx.common.compose.components.layout.MainScaffold
import moe.styx.common.compose.components.misc.SettingsCheckbox
import moe.styx.common.compose.components.misc.StringChoices
import moe.styx.common.compose.settings

class SettingsView : Screen {
    private val portraitCardChoices = listOf("3", "4", "5")
    private val landScapeCardChoices = listOf("Adaptive", "7", "8", "9", "10")

    @Composable
    override fun Content() {
        MainScaffold(Modifier.safeAreaPadding(), title = "Settings") {
            val scrollState = rememberScrollState()
            Column(Modifier.padding(8.dp).verticalScroll(scrollState, true)) {
                Text("Layout Options", modifier = Modifier.padding(5.dp), style = MaterialTheme.typography.titleLarge)
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
            }
        }
    }
}