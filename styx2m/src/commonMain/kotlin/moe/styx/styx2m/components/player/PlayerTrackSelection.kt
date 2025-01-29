package moe.styx.styx2m.components.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import moe.styx.common.extension.eqI
import moe.styx.styx2m.misc.Track
import moe.styx.styx2m.player.MediaPlayer

@Composable
fun TrackDropdownItem(track: Track, mediaPlayer: MediaPlayer, border: Boolean) {
    Column {
        if (border) {
            HorizontalDivider(Modifier.padding(5.dp, 3.dp).fillMaxWidth(), thickness = 2.dp)
        }
        Row(
            Modifier.padding(7.dp, 7.dp).height(IntrinsicSize.Min)
                .clickable(enabled = !track.selected || track.type eqI "sub") {
                    if (track.type eqI "sub")
                        if (track.selected)
                            mediaPlayer.setSubtitleTrack(-1)
                        else
                            mediaPlayer.setSubtitleTrack(track.id)
                    else
                        mediaPlayer.setAudioTrack(track.id)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${track.lang ?: "Unknown"}${if (track.title.isNullOrBlank()) "" else " | ${track.title}"}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Done, "Selected track",
                modifier = Modifier.padding(4.dp, 0.dp).size(20.dp).alpha(if (track.selected) 1f else 0f)
            )
        }
    }
}