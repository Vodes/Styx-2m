package moe.styx.styx2m.player

import moe.styx.common.util.Log
import moe.styx.styx2m.player.vlckit.VlcKitIosBackend
import moe.styx.styx2m.player.vlckit.instantiateVlcKitBridge

actual fun createPlayerBackend(sink: PlayerBackendSink): PlayerBackendWithSurface {
    val bridgeFactory = instantiateVlcKitBridge
    if (bridgeFactory == null) {
        Log.w("PlayerBackend") { "VLC bridge is not registered; using no-op iOS player backend." }
        return UnimplementedIosPlayerBackend(PlayerBackendId.VLC)
    }

    return VlcKitIosBackend(sink, bridgeFactory())
}
