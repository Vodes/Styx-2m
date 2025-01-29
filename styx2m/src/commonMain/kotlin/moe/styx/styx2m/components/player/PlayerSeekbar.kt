package moe.styx.styx2m.components.player

import SeekerDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.extension.containsAny
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.misc.ifInvalid
import moe.styx.styx2m.misc.secondsDurationString
import moe.styx.styx2m.player.MediaPlayer
import moe.styx.styx2m.player.seeker.Seeker
import moe.styx.styx2m.player.seeker.Segment
import moe.styx.styx2m.player.seeker.rememberSeekerState
import moe.styx.styx2m.theme.darkScheme
import kotlin.math.max
import kotlin.math.min

@Composable
fun TimelineControls(
    mediaPlayer: MediaPlayer,
    currentTime: Long,
    cacheTime: Long,
    duration: Long,
    chapters: List<Chapter>,
    onTap: () -> Unit
) {
    val seekerState = rememberSeekerState()
    var isDragging by remember { mutableStateOf(false) }
    var seekerValue by remember { mutableStateOf(0f) }
    Row(
        Modifier.padding(25.dp, 20.dp).clip(AppShapes.extraLarge).background(darkScheme.background.copy(0.5F))
            .fillMaxWidth(0.88F),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isDragging) seekerValue.toLong().secondsDurationString() else currentTime.secondsDurationString(),
            Modifier.padding(10.dp, 0.dp, 7.dp, 0.dp),
            color = darkScheme.onSurface
        )

        Seeker(
            Modifier.padding(20.dp, 5.dp).weight(1f),
            seekerState,
            value = max(currentTime.toFloat(), 0f).ifInvalid(0f),
            thumbValue = if (isDragging) seekerValue else max(currentTime.toFloat(), 0f).ifInvalid(0f),
            range = 0f..duration.toFloat().ifInvalid(1f),
            readAheadValue = cacheTime.toFloat().ifInvalid(0f),
            segments = chapters.map {
                Segment(
                    it.title,
                    it.time,
                    if (it.title.containsAny("op", "intro", "ed", "end", "credits"))
                        darkScheme.secondary
                    else
                        Color.Unspecified
                )
            },
            onValueChange = {
                isDragging = true
                seekerValue = it
                onTap()
            },
            onValueChangeFinished = {
                mediaPlayer.seek(max(min(seekerValue.toLong(), duration - 1), 0))
                isDragging = false
            },
            colors = SeekerDefaults.seekerColors(
                darkScheme.primary,
                darkScheme.surface.copy(0.8F),
                thumbColor = darkScheme.primary,
                readAheadColor = darkScheme.onSurface.copy(0.3F)
            )
        )

        Text(duration.secondsDurationString(), Modifier.padding(7.dp, 0.dp, 10.dp, 0.dp), color = darkScheme.onSurface)
    }
}