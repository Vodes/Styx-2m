package moe.styx.styx2m.views.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import moe.styx.common.compose.components.misc.Toggles
import moe.styx.common.compose.settings
import moe.styx.styx2m.theme.LocalThemeIsDark

private val themeChoices = listOf("System", "Light", "Dark")

@Composable
fun AppearanceSettings() {
    val systemIsDark = isSystemInDarkTheme()
    var currentlyDarkSelected by LocalThemeIsDark.current

    Toggles.ContainerRadioSelect(
        "Theme",
        value = settings["theme", "System"],
        choices = themeChoices
    ) {
        settings["theme"] = it

        val requestedDark = when (it.lowercase()) {
            "system" -> systemIsDark
            "light" -> false
            else -> true
        }
        currentlyDarkSelected = requestedDark
    }
    Toggles.ContainerSwitch("Favourites tab on start-up", value = settings["favs-startup", false]) { settings["favs-startup"] = it }
    Toggles.ContainerSwitch("Show names on cards", value = settings["display-names", false]) { settings["display-names"] = it }
    Spacer(Modifier.height(3.dp))
}