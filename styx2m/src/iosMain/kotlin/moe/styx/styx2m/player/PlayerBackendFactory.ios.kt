package moe.styx.styx2m.player

actual fun createPlayerBackend(sink: PlayerBackendSink): PlayerBackendWithSurface {
    return UnimplementedIosPlayerBackend()
}
