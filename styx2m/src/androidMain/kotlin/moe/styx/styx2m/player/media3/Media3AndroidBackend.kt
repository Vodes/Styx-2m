package moe.styx.styx2m.player.media3

import android.content.Context
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.github.peerless2012.ass.media.kt.buildWithAssSupport
import io.github.peerless2012.ass.media.type.AssRenderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.styx.common.data.MediaPreferences
import moe.styx.common.util.Log
import moe.styx.styx2m.player.PlaybackSource
import moe.styx.styx2m.player.PlaybackStatus
import moe.styx.styx2m.player.PlayerBackendCapabilities
import moe.styx.styx2m.player.PlayerBackendId
import moe.styx.styx2m.player.PlayerBackendSink
import moe.styx.styx2m.player.PlayerBackendWithSurface
import moe.styx.styx2m.player.PlayerTrack
import moe.styx.styx2m.player.PlayerTrackType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class Media3AndroidBackend(
    private val sink: PlayerBackendSink
) : PlayerBackendWithSurface {
    override val id = PlayerBackendId.MEDIA3
    override val capabilities = PlayerBackendCapabilities(
        supportsCacheEnd = false,
        supportsChapters = false,
        supportsAudioTrackSelection = true,
        supportsSubtitleTrackSelection = true,
        supportsLocalFiles = true,
        supportsHttpStreams = true
    )

    private var player: ExoPlayer? = null
    private var context: Context? = null
    private var pendingLoad: Pair<PlaybackSource, Long>? = null
    private var progressJob: Job? = null
    private var metadataJob: Job? = null
    private var playbackStatus: PlaybackStatus = PlaybackStatus.Idle
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val trackRefsById = mutableMapOf<Int, TrackRef>()

    override fun load(source: PlaybackSource, startAt: Long) {
        val player = player
        if (player == null) {
            pendingLoad = source to startAt
            return
        }

        when (source) {
            is PlaybackSource.LocalFile -> loadUri(player, Uri.fromFile(File(source.path)), startAt, readLocalMetadataFallback = true)
            is PlaybackSource.RemoteUrl -> loadUri(player, Uri.parse(source.url), startAt, readLocalMetadataFallback = false)
            is PlaybackSource.Unavailable -> showMessage(source.message)
        }
    }

    override fun setPlaying(playing: Boolean) {
        val player = player ?: return
        if (playing) {
            player.play()
        } else {
            player.pause()
        }
    }

    override fun seek(position: Long) {
        val player = player ?: return
        updateStatus(PlaybackStatus.Seeking)
        player.seekTo(position.coerceAtLeast(0) * 1000)
    }

    override fun setAudioTrack(id: Int) {
        selectTrack(id)
    }

    override fun setSubtitleTrack(id: Int) {
        if (id == -1) {
            clearTrackOverrides(C.TRACK_TYPE_TEXT)
        } else {
            selectTrack(id)
        }
    }

    override fun showMessage(message: String, durationMillis: Int) {
        Log.w("Media3") { message }
    }

    override fun release() {
        progressJob?.cancel()
        progressJob = null
        metadataJob?.cancel()
        metadataJob = null
        runCatching {
            player?.removeListener(listener)
            player?.release()
        }.onFailure {
            sink.onError("Media3", "Failed to release Media3 backend.", it)
        }
        player = null
        context = null
        pendingLoad = null
        playbackStatus = PlaybackStatus.Idle
        trackRefsById.clear()
    }

    @Composable
    override fun RenderSurface(modifier: Modifier, preferences: MediaPreferences?) {
        val context = LocalContext.current
        val player = remember(context) { ensureInitialized(context) }

        DisposableEffect(player) {
            onDispose {
                release()
            }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    applyBlackLetterboxBackground()
                    this.player = player
                }
            },
            update = { playerView ->
                playerView.applyBlackLetterboxBackground()
                playerView.player = player
            },
            modifier = modifier.background(ComposeColor.Black).fillMaxSize()
        )
    }

    private fun ensureInitialized(context: Context): ExoPlayer {
        player?.let { return it }
        this.context = context.applicationContext

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
        val dataSourceFactory = StyxMedia3DataSourceFactory(
            upstreamFactory = DefaultDataSource.Factory(context),
            onMatroskaTitle = ::onMatroskaTitle
        )
        val newPlayer = ExoPlayer.Builder(context)
            .buildWithAssSupport(
                context = context,
                renderType = AssRenderType.CUES,
                subtitleView = null,
                dataSourceFactory = dataSourceFactory,
                extractorsFactory = DefaultExtractorsFactory(),
                renderersFactory = renderersFactory
            )
            .apply { addListener(listener) }
        player = newPlayer

        pendingLoad?.let { (source, startAt) ->
            pendingLoad = null
            load(source, startAt)
        }

        return newPlayer
    }

    private fun loadUri(player: ExoPlayer, uri: Uri, startAt: Long, readLocalMetadataFallback: Boolean) {
        trackRefsById.clear()
        loadEmbeddedTitle(uri, readLocalMetadataFallback)
        updateStatus(PlaybackStatus.Buffering)
        player.setMediaItem(MediaItem.fromUri(uri), startAt.coerceAtLeast(0) * 1000)
        player.prepare()
        player.playWhenReady = true
    }

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                player?.mediaMetadata?.let(::logMediaMetadata)
            }
            updateStatus(playbackState.toPlaybackStatus(player?.isPlaying == true))
            syncProgress()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val playbackState = player?.playbackState ?: Player.STATE_IDLE
            updateStatus(playbackState.toPlaybackStatus(isPlaying))
            syncProgress()
        }

        override fun onTracksChanged(tracks: Tracks) {
            logTracks(tracks)
            syncTracks(tracks)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d("Media3") {
                "Media item transition: reason=$reason, mediaId=${mediaItem?.mediaId}, " +
                    "localUri=${mediaItem?.localConfiguration?.uri}, requestMetadata=${mediaItem?.requestMetadata}, " +
                    "mediaMetadata=${mediaItem?.mediaMetadata?.describeForLog()}"
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            logMediaMetadata(mediaMetadata)
            syncTitle(mediaMetadata)
        }

        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
            Log.d("Media3") { "Playlist metadata changed: ${mediaMetadata.describeForLog()}" }
        }

        override fun onMetadata(metadata: Metadata) {
            Log.d("Media3") {
                "Raw metadata changed: presentationTimeUs=${metadata.presentationTimeUs}, entries=${metadata.describeEntries()}"
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            sink.onError("Media3", "Media3 playback error.", error)
        }
    }

    private fun updateStatus(status: PlaybackStatus) {
        if (playbackStatus == status) return
        playbackStatus = status
        sink.onPlaybackStatus(status)
        if (status == PlaybackStatus.Playing) {
            startProgressUpdates()
        } else {
            progressJob?.cancel()
            progressJob = null
        }
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (true) {
                syncProgress()
                delay(PROGRESS_UPDATE_MS)
            }
        }
    }

    private fun syncProgress() {
        val player = player ?: return
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        val position = player.currentPosition.coerceAtLeast(0L)
        sink.onProgress(position / 1000)
        sink.onDuration(duration.coerceAtLeast(0L) / 1000)
        sink.onPlaybackPercent(if (duration > 0) position.toFloat() / duration.toFloat() * 100F else 0F)
    }

    private fun syncTracks(tracks: Tracks) {
        trackRefsById.clear()
        val normalizedTracks = buildList {
            tracks.groups.forEachIndexed { groupIndex, group ->
                val type = group.mediaTrackGroup.type.toPlayerTrackType()
                if (type == PlayerTrackType.UNKNOWN || type == PlayerTrackType.VIDEO) return@forEachIndexed

                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val id = trackId(groupIndex, trackIndex)
                    trackRefsById[id] = TrackRef(group.mediaTrackGroup, trackIndex)
                    add(
                        PlayerTrack(
                            id = id,
                            type = type,
                            title = format.label,
                            language = format.language,
                            isSelected = group.isTrackSelected(trackIndex),
                            codec = format.codecs,
                            channels = if (type == PlayerTrackType.AUDIO) format.channelCount.takeIf { it != -1 } else null
                        )
                    )
                }
            }
        }
        sink.onTracks(normalizedTracks)
    }

    private fun PlayerView.applyBlackLetterboxBackground() {
        setBackgroundColor(Color.BLACK)
        setShutterBackgroundColor(Color.BLACK)
        findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)?.setBackgroundColor(Color.BLACK)
    }

    private fun logMediaMetadata(mediaMetadata: MediaMetadata) {
        Log.d("Media3") {
            "MediaMetadata changed: title=${mediaMetadata.title.quoteForLog()}, " +
                "displayTitle=${mediaMetadata.displayTitle.quoteForLog()}, " +
                "subtitle=${mediaMetadata.subtitle.quoteForLog()}, " +
                "description=${mediaMetadata.description.quoteForLog()}, " +
                "durationMs=${mediaMetadata.durationMs}, " +
                "mediaType=${mediaMetadata.mediaType}, " +
                "extras=${mediaMetadata.extras.describeForLog()}"
        }
    }

    private fun loadEmbeddedTitle(uri: Uri, readLocalMetadataFallback: Boolean) {
        val context = context ?: return
        metadataJob?.cancel()
        if (!readLocalMetadataFallback) {
            Log.d("Media3") { "Skipping MediaMetadataRetriever fallback for remote source: $uri" }
            return
        }

        metadataJob = scope.launch {
            val metadata = withContext(Dispatchers.IO) {
                readEmbeddedMetadata(context, uri)
            }
            val title = metadata[RETRIEVER_KEY_TITLE]?.trim()

            Log.d("Media3") { "MediaMetadataRetriever metadata for $uri: ${metadata.describeForLog()}" }
            if (!title.isNullOrBlank()) {
                sink.onMediaTitle(title)
            }
        }
    }

    private fun readEmbeddedMetadata(context: Context, uri: Uri): Map<String, String> {
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)

                RETRIEVER_KEYS.mapNotNull { (key, label) ->
                    retriever.extractMetadata(key)?.trim()?.takeIf(String::isNotBlank)?.let { label to it }
                }.toMap()
            }
        }.onFailure {
            Log.w("Media3") { "Failed to read embedded metadata with MediaMetadataRetriever for $uri: ${it.message}" }
        }.getOrDefault(emptyMap())
    }

    private fun syncTitle(mediaMetadata: MediaMetadata) {
        val title = mediaMetadata.title?.toString()?.trim()
            ?: return

        if (title.isNotBlank()) {
            sink.onMediaTitle(title)
        }
    }

    private fun onMatroskaTitle(title: String) {
        val normalizedTitle = title.trim()
        Log.d("Media3") { "Matroska segment title: ${normalizedTitle.quoteForLog()}" }
        if (normalizedTitle.isNotBlank()) {
            sink.onMediaTitle(normalizedTitle)
        }
    }

    private fun selectTrack(id: Int) {
        val player = player ?: return
        val ref = trackRefsById[id] ?: return
        val override = TrackSelectionOverride(ref.group, ref.trackIndex)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
    }

    private fun clearTrackOverrides(trackType: Int) {
        val player = player ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(trackType)
            .build()
    }

    private fun Int.toPlayerTrackType(): PlayerTrackType = when (this) {
        C.TRACK_TYPE_AUDIO -> PlayerTrackType.AUDIO
        C.TRACK_TYPE_TEXT -> PlayerTrackType.SUBTITLE
        C.TRACK_TYPE_VIDEO -> PlayerTrackType.VIDEO
        else -> PlayerTrackType.UNKNOWN
    }

    private fun Int.toPlaybackStatus(isPlaying: Boolean): PlaybackStatus = when (this) {
        Player.STATE_BUFFERING -> PlaybackStatus.Buffering
        Player.STATE_READY -> if (isPlaying) PlaybackStatus.Playing else PlaybackStatus.Paused
        Player.STATE_ENDED -> PlaybackStatus.EOF
        Player.STATE_IDLE -> PlaybackStatus.Idle
        else -> PlaybackStatus.Idle
    }

    private fun CharSequence?.quoteForLog(): String = this?.toString()?.let { "'$it'" } ?: "<null>"

    private fun MediaMetadata.describeForLog(): String {
        return "title=${title.quoteForLog()}, " +
            "displayTitle=${displayTitle.quoteForLog()}, " +
            "subtitle=${subtitle.quoteForLog()}, " +
            "description=${description.quoteForLog()}, " +
            "durationMs=$durationMs, mediaType=$mediaType, extras=${extras.describeForLog()}"
    }

    private fun android.os.Bundle?.describeForLog(): String {
        val bundle = this ?: return "<null>"
        return bundle.keySet().joinToString(prefix = "{", postfix = "}") { key ->
            "$key=${bundle.get(key)}"
        }
    }

    private fun Map<String, String>.describeForLog(): String {
        return entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key='$value'" }
    }

    private fun Metadata.describeEntries(): String {
        return (0 until length()).joinToString(prefix = "[", postfix = "]") { index ->
            val entry = get(index)
            "${entry.javaClass.name}: $entry"
        }
    }

    private fun logTracks(tracks: Tracks) {
        Log.d("Media3") {
            buildString {
                append("Tracks changed: groupCount=${tracks.groups.size}")
                tracks.groups.forEachIndexed { groupIndex, group ->
                    append("; group[$groupIndex]={")
                    append("type=${group.mediaTrackGroup.type}, length=${group.length}, adaptive=${group.isAdaptiveSupported}")
                    for (trackIndex in 0 until group.length) {
                        append(", track[$trackIndex]=")
                    append(group.getTrackFormat(trackIndex).describeForLog(group.mediaTrackGroup.type))
                    }
                    append("}")
                }
            }
        }
    }

    private fun Format.describeForLog(trackType: Int): String {
        val mediaSpecific = when (trackType) {
            C.TRACK_TYPE_VIDEO -> ", width=$width, height=$height, frameRate=$frameRate"
            C.TRACK_TYPE_AUDIO -> ", channelCount=$channelCount, sampleRate=$sampleRate"
            else -> ""
        }
        return "{id=$id, label=$label, labels=$labels, language=$language, " +
            "selectionFlags=$selectionFlags, roleFlags=$roleFlags, codecs=$codecs, " +
            "containerMimeType=$containerMimeType, sampleMimeType=$sampleMimeType$mediaSpecific, " +
            "metadata=${metadata?.describeEntries() ?: "<null>"}, customData=$customData}"
    }

    private data class TrackRef(
        val group: androidx.media3.common.TrackGroup,
        val trackIndex: Int
    )

    private companion object {
        const val PROGRESS_UPDATE_MS = 500L
        const val RETRIEVER_KEY_TITLE = "title"

        val RETRIEVER_KEYS = listOf(
            MediaMetadataRetriever.METADATA_KEY_TITLE to RETRIEVER_KEY_TITLE,
            MediaMetadataRetriever.METADATA_KEY_DATE to "date",
            MediaMetadataRetriever.METADATA_KEY_DURATION to "duration",
            MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS to "numTracks",
            MediaMetadataRetriever.METADATA_KEY_MIMETYPE to "mimeType",
            MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO to "hasAudio",
            MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO to "hasVideo",
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH to "videoWidth",
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT to "videoHeight",
            MediaMetadataRetriever.METADATA_KEY_BITRATE to "bitrate",
            MediaMetadataRetriever.METADATA_KEY_LOCATION to "location",
            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION to "videoRotation",
            MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE to "captureFramerate"
        )

        fun trackId(groupIndex: Int, trackIndex: Int): Int {
            return groupIndex * 1000 + trackIndex
        }
    }
}

private class StyxMedia3DataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val onMatroskaTitle: (String) -> Unit
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return MatroskaTitleObservingDataSource(upstreamFactory.createDataSource(), onMatroskaTitle)
    }
}

private class MatroskaTitleObservingDataSource(
    private val upstream: DataSource,
    private val onMatroskaTitle: (String) -> Unit
) : DataSource {
    private var parser: MatroskaTitleParser? = null

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        parser = if (dataSpec.position == 0L) MatroskaTitleParser(onMatroskaTitle) else null
        return upstream.open(dataSpec)
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val bytesRead = upstream.read(buffer, offset, length)
        if (bytesRead > 0) {
            parser?.consume(buffer, offset, bytesRead)
        }
        return bytesRead
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    @Throws(IOException::class)
    override fun close() {
        parser = null
        upstream.close()
    }
}

private class MatroskaTitleParser(
    private val onTitle: (String) -> Unit
) {
    private val buffer = ByteArrayOutputStream()
    private var done = false

    fun consume(source: ByteArray, offset: Int, length: Int) {
        if (done || buffer.size() >= MAX_SCAN_BYTES) return

        val appendLength = minOf(length, MAX_SCAN_BYTES - buffer.size())
        buffer.write(source, offset, appendLength)
        scan(buffer.toByteArray())
    }

    private fun scan(bytes: ByteArray) {
        val clusterIndex = bytes.indexOf(CLUSTER_ID)
        val searchLimit = if (clusterIndex >= 0) clusterIndex else bytes.size
        var index = bytes.indexOf(TITLE_ID, endExclusive = searchLimit)

        while (index >= 0) {
            val length = readEbmlVint(bytes, index + TITLE_ID.size) ?: return
            val valueStart = index + TITLE_ID.size + length.byteCount
            if (length.value > MAX_TITLE_BYTES) {
                index = bytes.indexOf(TITLE_ID, startIndex = index + 1, endExclusive = searchLimit)
                continue
            }

            val valueEnd = valueStart + length.value.toInt()
            if (valueEnd > bytes.size) return

            val title = bytes.copyOfRange(valueStart, valueEnd).toString(Charsets.UTF_8).trim()
            if (title.isLikelyTitle()) {
                done = true
                onTitle(title)
                return
            }

            index = bytes.indexOf(TITLE_ID, startIndex = index + 1, endExclusive = searchLimit)
        }

        if (clusterIndex >= 0 || bytes.size >= MAX_SCAN_BYTES) {
            done = true
        }
    }

    private fun readEbmlVint(bytes: ByteArray, offset: Int): Vint? {
        if (offset >= bytes.size) return null

        val firstByte = bytes[offset].toInt() and 0xFF
        if (firstByte == 0) return null

        var marker = 0x80
        var byteCount = 1
        while (byteCount <= 8 && firstByte and marker == 0) {
            marker = marker shr 1
            byteCount += 1
        }
        if (byteCount > 8 || offset + byteCount > bytes.size) return null

        var value = (firstByte and (marker - 1)).toLong()
        for (i in 1 until byteCount) {
            value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }

        return Vint(byteCount, value)
    }

    private fun String.isLikelyTitle(): Boolean {
        return isNotBlank() && length <= MAX_TITLE_CHARS && !contains('\uFFFD') && none { it.isISOControl() && it !in "\t\r\n" }
    }

    private data class Vint(val byteCount: Int, val value: Long)

    private companion object {
        const val MAX_SCAN_BYTES = 512 * 1024
        const val MAX_TITLE_BYTES = 4096L
        const val MAX_TITLE_CHARS = 512

        val TITLE_ID = byteArrayOf(0x7B, 0xA9.toByte())
        val CLUSTER_ID = byteArrayOf(0x1F, 0x43, 0xB6.toByte(), 0x75)
    }
}

private fun ByteArray.indexOf(
    needle: ByteArray,
    startIndex: Int = 0,
    endExclusive: Int = size
): Int {
    if (needle.isEmpty()) return startIndex.coerceAtMost(endExclusive)
    val lastStart = endExclusive - needle.size
    if (lastStart < startIndex) return -1

    for (index in startIndex..lastStart) {
        var matched = true
        for (needleIndex in needle.indices) {
            if (this[index + needleIndex] != needle[needleIndex]) {
                matched = false
                break
            }
        }
        if (matched) return index
    }

    return -1
}
