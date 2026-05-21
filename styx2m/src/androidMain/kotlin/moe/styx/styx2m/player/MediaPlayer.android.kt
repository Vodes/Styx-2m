package moe.styx.styx2m.player

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaPreferences
import moe.styx.common.util.Log
import moe.styx.styx2m.misc.Chapter
import moe.styx.styx2m.misc.findActivity
import moe.styx.styx2m.player.mpv.MpvAndroidBackend

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) :
    AMediaPlayer(initialEntryID, startAt),
    PlayerBackendSink {

    private val sinkScope = CoroutineScope(Dispatchers.Main)
    private val backend: PlayerBackend = MpvAndroidBackend(this)
    private var shouldApplyInitialStart = true

    actual override fun setPlaying(playing: Boolean) {
        backend.setPlaying(playing)
    }

    actual override fun seek(position: Long) {
        backend.seek(position)
    }

    @Composable
    actual override fun requestRotationLock() {
        val context = LocalContext.current
        val activity = context.findActivity()
        LaunchedEffect(Unit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            Log.d { "Requesting locked rotation: ${activity != null}" }
        }
        DisposableEffect(Unit) {
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                Log.d { "Releasing locked rotation: ${activity != null}" }
            }
        }
    }

    @Composable
    actual override fun releaseRotationLock() {
        val context = LocalContext.current
        val activity = context.findActivity()
        LaunchedEffect(Unit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            Log.d { "Releasing locked rotation: ${activity != null}" }
        }
    }

    actual override fun setSubtitleTrack(id: Int) {
        backend.setSubtitleTrack(id)
    }

    actual override fun setAudioTrack(id: Int) {
        backend.setAudioTrack(id)
    }

    actual override fun internalPlayEntry(mediaEntry: MediaEntry, scope: CoroutineScope) {
        scope.launch {
            currentEntry.emit(mediaEntry.GUID)
        }
    }

    @Composable
    actual override fun PlayerComponent(entryList: List<MediaEntry>, preferences: MediaPreferences?) {
        val curEntryID by currentEntry.collectAsState()
        backend.RenderSurface(Modifier, preferences)

        LaunchedEffect(curEntryID) {
            val source = PlaybackSourceResolver.resolve(curEntryID, entryList)
            // Preserve the small delay that was previously needed for downloaded files.
            if (source is PlaybackSource.LocalFile) {
                withContext(Dispatchers.IO) {
                    Thread.sleep(1)
                }
            }
            val entryStart = if (shouldApplyInitialStart && curEntryID == initialEntryID) startAt else 0L
            shouldApplyInitialStart = false
            backend.load(source, entryStart)
        }
    }

    actual override fun releasePlayer() {
        backend.release()
    }

    override fun onPlaybackStatus(status: PlaybackStatus) {
        sinkScope.launch { playbackStatus.emit(status) }
    }

    override fun onMediaTitle(title: String) {
        sinkScope.launch { mediaTitle.emit(title) }
    }

    override fun onProgress(seconds: Long) {
        sinkScope.launch { progress.emit(seconds) }
    }

    override fun onDuration(seconds: Long) {
        sinkScope.launch { fileLength.emit(seconds) }
    }

    override fun onCacheEnd(seconds: Long) {
        sinkScope.launch { cacheEnd.emit(seconds) }
    }

    override fun onPlaybackPercent(percent: Float) {
        playbackPercent = percent
    }

    override fun onTracks(tracks: List<PlayerTrack>) {
        sinkScope.launch { trackList.emit(tracks) }
    }

    override fun onChapters(chapters: List<Chapter>) {
        sinkScope.launch { this@MediaPlayer.chapters.emit(chapters) }
    }

    override fun onError(source: String, message: String, throwable: Throwable?) {
        Log.e(source, throwable) { message }
    }
}
