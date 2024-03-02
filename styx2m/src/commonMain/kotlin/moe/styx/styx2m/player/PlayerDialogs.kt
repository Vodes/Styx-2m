package moe.styx.styx2m.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import moe.styx.common.compose.components.AppShapes
import moe.styx.common.compose.components.misc.TextWithCheckBox
import moe.styx.common.extension.eqI
import moe.styx.styx2m.misc.Track

@Composable
fun TracklistDialog(mediaPlayer: MediaPlayer, tracklist: List<Track>, onDismiss: () -> Unit) {
    Dialog(onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val scrollState = rememberScrollState()
        Surface(
            Modifier.fillMaxWidth(0.85F).fillMaxHeight(0.95F).widthIn(0.dp, 900.dp).clip(AppShapes.extraLarge).padding(30.dp),
            color = MaterialTheme.colorScheme.background.copy(0.96F)
        ) {
            Column(
                Modifier.fillMaxSize().padding(22.dp).verticalScroll(scrollState)
            ) {
                Text("Audio Tracks", Modifier.padding(0.dp, 0.dp, 0.dp, 6.dp), style = MaterialTheme.typography.titleMedium)
                for (audioTrack in tracklist.filter { it.type eqI "audio" }) {
                    Row(Modifier.padding(10.dp, 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextWithCheckBox(
                            "${audioTrack.lang?.uppercase()} | ${audioTrack.title}",
                            audioTrack.selected,
                            enabled = !audioTrack.selected
                        ) {
                            mediaPlayer.setAudioTrack(audioTrack.id)
                        }
                    }
                }
                HorizontalDivider(Modifier.fillMaxWidth().padding(8.dp))
                Text("Subtitle Tracks", Modifier.padding(0.dp, 6.dp, 0.dp, 6.dp), style = MaterialTheme.typography.titleMedium)
                for (subTrack in tracklist.filter { it.type eqI "sub" }) {
                    Row(Modifier.padding(10.dp, 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextWithCheckBox(
                            "${subTrack.lang?.uppercase()} | ${subTrack.title}",
                            subTrack.selected,
                            enabled = !subTrack.selected
                        ) {
                            mediaPlayer.setSubtitleTrack(subTrack.id)
                        }
                    }
                }
            }
        }
    }
}