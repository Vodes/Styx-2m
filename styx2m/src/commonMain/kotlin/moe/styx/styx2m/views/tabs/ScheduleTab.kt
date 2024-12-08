package moe.styx.styx2m.views.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import moe.styx.common.compose.components.schedule.ScheduleViewComponent
import moe.styx.common.compose.extensions.createTabOptions
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.styx2m.misc.pushMediaView

class ScheduleTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            return createTabOptions("Schedule", Icons.Default.CalendarViewWeek)
        }

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        ScheduleViewComponent {
            nav.pushMediaView(it, false)
        }
    }
}