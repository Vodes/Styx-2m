package moe.styx.styx2m.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.styx.common.compose.utils.MpvPreferences
import moe.styx.common.data.MediaEntry
import moe.styx.common.data.MediaPreferences
import moe.styx.common.util.Log
import moe.styx.styx2m.misc.Chapter

actual class MediaPlayer actual constructor(initialEntryID: String, startAt: Long) :
    AMediaPlayer(initialEntryID, startAt),
    PlayerBackendSink {

    private val sinkScope = CoroutineScope(Dispatchers.Main)
    private val backend: PlayerBackendWithSurface = createPlayerBackend(this)
    private var shouldApplyInitialStart = true
    private var mediaPreferences: MediaPreferences? = null
    private var manualAudioOverrideId: Int? = null
    private var manualSubtitleOverrideId: Int? = null
    private var lastAutomaticTrackSelectionKey: String? = null

    actual override fun setPlaying(playing: Boolean) {
        backend.setPlaying(playing)
    }

    actual override fun seek(position: Long) {
        backend.seek(position)
    }

    actual override fun setSubtitleTrack(id: Int) {
        manualSubtitleOverrideId = id
        lastAutomaticTrackSelectionKey = null
        backend.setSubtitleTrack(id)
    }

    actual override fun setAudioTrack(id: Int) {
        manualAudioOverrideId = id
        lastAutomaticTrackSelectionKey = null
        backend.setAudioTrack(id)
    }

    actual override fun internalPlayEntry(mediaEntry: MediaEntry, scope: CoroutineScope) {
        manualAudioOverrideId = null
        manualSubtitleOverrideId = null
        lastAutomaticTrackSelectionKey = null
        scope.launch {
            currentEntry.emit(mediaEntry.GUID)
        }
    }

    @Composable
    actual override fun PlayerComponent(entryList: List<MediaEntry>, preferences: MediaPreferences?) {
        val curEntryID by currentEntry.collectAsState()
        mediaPreferences = preferences
        backend.RenderSurface(Modifier, preferences)

        LaunchedEffect(preferences) {
            mediaPreferences = preferences
            lastAutomaticTrackSelectionKey = null
            applyAutomaticTrackSelection(trackList.value)
        }

        LaunchedEffect(curEntryID) {
            val source = PlaybackSourceResolver.resolve(curEntryID, entryList)
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
        sinkScope.launch {
            trackList.emit(tracks)
            applyAutomaticTrackSelection(tracks)
        }
    }

    override fun onChapters(chapters: List<Chapter>) {
        sinkScope.launch { this@MediaPlayer.chapters.emit(chapters) }
    }

    override fun onError(source: String, message: String, throwable: Throwable?) {
        Log.e(source, throwable) { message }
    }

    private fun applyAutomaticTrackSelection(tracks: List<PlayerTrack>) {
        if (backend.id != PlayerBackendId.VLC) return
        if (tracks.none { it.type == PlayerTrackType.AUDIO || it.type == PlayerTrackType.SUBTITLE }) return

        val key = automaticTrackSelectionKey(tracks)
        if (lastAutomaticTrackSelectionKey == key) return
        lastAutomaticTrackSelectionKey = key

        val result = TrackPreferenceSelector.select(
            TrackSelectionInput(
                tracks = tracks,
                mediaPreferences = mediaPreferences,
                languagePreferences = playerLanguagePreferences(),
                manualAudioOverrideId = manualAudioOverrideId,
                manualSubtitleOverrideId = manualSubtitleOverrideId
            )
        )

        if (manualAudioOverrideId == null && result.audioTrackId != null) {
            val selectedAudioId = tracks.firstOrNull { it.type == PlayerTrackType.AUDIO && it.isSelected }?.id
            if (selectedAudioId != result.audioTrackId) {
                backend.setAudioTrack(result.audioTrackId)
            }
        }

        if (manualSubtitleOverrideId == null) {
            val selectedSubtitleId = tracks.firstOrNull { it.type == PlayerTrackType.SUBTITLE && it.isSelected }?.id
            when {
                result.subtitlesDisabled && selectedSubtitleId != null -> backend.setSubtitleTrack(-1)
                !result.subtitlesDisabled && result.subtitleTrackId != null && selectedSubtitleId != result.subtitleTrackId ->
                    backend.setSubtitleTrack(result.subtitleTrackId)
            }
        }
    }

    private fun playerLanguagePreferences(): PlayerLanguagePreferences {
        val pref = MpvPreferences.getOrDefault()
        return PlayerLanguagePreferences(
            preferredAudioLanguages = pref.getAlangArg(mediaPreferences).split(","),
            preferredSubtitleLanguages = pref.getSlangArg(mediaPreferences).split(",")
        )
    }

    private fun automaticTrackSelectionKey(tracks: List<PlayerTrack>): String {
        return buildString {
            append(currentEntry.value)
            append('|')
            append(mediaPreferences.hashCode())
            append('|')
            append(manualAudioOverrideId)
            append('|')
            append(manualSubtitleOverrideId)
            tracks.forEach { track ->
                append('|')
                append(track.id)
                append(':')
                append(track.type)
                append(':')
                append(track.language)
                append(':')
                append(track.title)
                append(':')
                append(track.isDefault)
                append(':')
                append(track.isForced)
                append(':')
                append(track.isSelected)
            }
        }
    }
}
