package moe.styx.styx2m.views.tabs

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import moe.styx.common.compose.components.schedule.ScheduleDay
import moe.styx.common.compose.extensions.createTabOptions
import moe.styx.common.compose.utils.LocalGlobalNavigator
import moe.styx.common.data.ScheduleWeekday
import moe.styx.styx2m.views.anime.AnimeDetailView

class ScheduleTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            return createTabOptions("Schedule", Icons.Default.CalendarViewWeek)
        }

    @Composable
    override fun Content() {
        val nav = LocalGlobalNavigator.current
        val days = ScheduleWeekday.entries.toTypedArray()
        LazyColumn {
            items(items = days, itemContent = { day ->
                ScheduleDay(day) { nav.push(AnimeDetailView(it.GUID)) }
            })
        }
    }
}