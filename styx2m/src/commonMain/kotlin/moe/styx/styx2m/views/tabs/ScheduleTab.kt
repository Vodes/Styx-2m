package moe.styx.styx2m.views.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import moe.styx.common.Platform
import moe.styx.common.compose.components.schedule.ScheduleViewComponent
import moe.styx.common.compose.extensions.createTabOptions
import moe.styx.common.compose.navigation.Tab
import moe.styx.common.compose.navigation.TabOptions
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.compose.utils.LocalLayoutSize
import moe.styx.styx2m.misc.pushMediaView
import moe.styx.styx2m.views.FloatingBottomNavContentPadding

class ScheduleTab : Tab {
    override val index: UInt
        get() = 3u

    override val options: TabOptions
        @Composable
        get() {
            return createTabOptions("Schedule", Icons.Default.CalendarViewWeek, index)
        }

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val sizes = LocalLayoutSize.current
        val bottomPadding = if (!sizes.isWide && Platform.current == Platform.IOS) FloatingBottomNavContentPadding else 0.dp
        ScheduleViewComponent(contentPadding = PaddingValues(bottom = bottomPadding)) {
            nav.pushMediaView(it, false)
        }
    }
}
