package moe.styx.styx2m.player.vlckit

import platform.UIKit.UIView

var instantiateVlcKitBridge: (() -> VlcKitPlayerBridge)? = null

abstract class VlcKitPlayerBridge {
    var onPropertyChange: ((name: String, value: Any?) -> Unit)? = null
    var onEvent: ((eventName: String) -> Unit)? = null

    abstract fun create(slang: String, alang: String, hardwareDecoding: Boolean)
    abstract fun destroy()
    abstract fun getPlayerView(): UIView
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
