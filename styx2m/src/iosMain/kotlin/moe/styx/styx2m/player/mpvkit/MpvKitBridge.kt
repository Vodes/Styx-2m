package moe.styx.styx2m.player.mpvkit

import platform.UIKit.UIView

var instantiateMpvKitBridge: (() -> MpvKitPlayerBridge)? = null

abstract class MpvKitPlayerBridge {
    var onPropertyChange: ((name: String, value: Any?) -> Unit)? = null
    var onEvent: ((eventName: String) -> Unit)? = null

    abstract fun create(
        videoOutputDriver: String,
        gpuApi: String,
        profile: String,
        hwdec: String,
        deband: Boolean,
        debandIterations: String,
        dither10bit: Boolean,
        blendSubtitles: Boolean,
        slang: String,
        alang: String,
    )
    abstract fun destroy()
    abstract fun getPlayerView(gpuApi: String): UIView
    abstract fun resizeSurface(width: Int, height: Int)

    abstract fun loadFile(path: String, startAt: Double)
    abstract fun loadUrl(url: String, startAt: Double)
    abstract fun setPaused(paused: Boolean)
    abstract fun seekTo(positionSeconds: Double)
    abstract fun setAudioTrack(id: Int)
    abstract fun setSubtitleTrack(id: Int)
    abstract fun showMessage(message: String, durationMillis: Int)

    abstract fun getTrackCount(): Int
    abstract fun getTrackType(index: Int): String?
    abstract fun getTrackId(index: Int): Int
    abstract fun getTrackLang(index: Int): String?
    abstract fun getTrackTitle(index: Int): String?
    abstract fun getTrackCodec(index: Int): String?
    abstract fun isTrackDefault(index: Int): Boolean
    abstract fun isTrackForced(index: Int): Boolean
    abstract fun isTrackSelected(index: Int): Boolean

    abstract fun getChapterCount(): Int
    abstract fun getChapterTitle(index: Int): String?
    abstract fun getChapterTime(index: Int): Double
}
