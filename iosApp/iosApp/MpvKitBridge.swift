import Foundation
import GLKit
import OpenGLES
import QuartzCore
import UIKit
import Libmpv
import ComposeApp

final class MPVMetalLayer: CAMetalLayer {
    override var drawableSize: CGSize {
        get { super.drawableSize }
        set {
            if Int(newValue.width) > 1 && Int(newValue.height) > 1 {
                super.drawableSize = newValue
            }
        }
    }
}

final class MPVRenderView: UIView {
    let metalLayer = MPVMetalLayer()
    private var lastLoggedBounds = CGSize.zero

    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        isUserInteractionEnabled = false
        clipsToBounds = true
        layer.masksToBounds = true
        autoresizingMask = [.flexibleWidth, .flexibleHeight]
        metalLayer.contentsScale = UIScreen.main.nativeScale
        metalLayer.framebufferOnly = true
        metalLayer.backgroundColor = UIColor.black.cgColor
        layer.addSublayer(metalLayer)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("MPVRenderView is code-instantiated only")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        layoutMetalLayer()
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        metalLayer.contentsScale = window?.screen.nativeScale ?? UIScreen.main.nativeScale
        layoutMetalLayer()
    }

    private func layoutMetalLayer() {
        let scale = window?.screen.nativeScale ?? UIScreen.main.nativeScale
        let drawableSize = CGSize(width: bounds.width * scale, height: bounds.height * scale)

        CATransaction.begin()
        CATransaction.setDisableActions(true)
        metalLayer.contentsScale = scale
        metalLayer.bounds = CGRect(origin: .zero, size: bounds.size)
        metalLayer.position = CGPoint(x: bounds.midX, y: bounds.midY)
        metalLayer.drawableSize = drawableSize
        CATransaction.commit()

        if bounds.size != lastLoggedBounds {
            lastLoggedBounds = bounds.size
            let windowBounds = window?.bounds ?? .zero
            print("[MPVKit] render view frame=\(frame) bounds=\(bounds) layerFrame=\(metalLayer.frame) drawable=\(metalLayer.drawableSize) window=\(windowBounds) scale=\(metalLayer.contentsScale)")
        }
    }
}

final class MPVOpenGLRenderView: GLKView, GLKViewDelegate {
    weak var bridge: MpvKitBridgeImpl?

    init() {
        let context = EAGLContext(api: .openGLES2)!
        super.init(frame: .zero, context: context)
        delegate = self
        backgroundColor = .black
        isUserInteractionEnabled = false
        enableSetNeedsDisplay = false
        autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("MPVOpenGLRenderView is code-instantiated only")
    }

    func glkView(_ view: GLKView, drawIn rect: CGRect) {
        bridge?.renderOpenGLFrame(in: view)
    }
}

final class MpvKitBridgeImpl: MpvKitPlayerBridge, @unchecked Sendable {
    private var mpv: OpaquePointer?
    private var mpvGL: OpaquePointer?
    private var renderView: MPVRenderView?
    private var openGLView: MPVOpenGLRenderView?
    private var hasLoadedFile = false
    private var lastSurfaceSize = CGSize.zero
    private let eventQueue = DispatchQueue(label: "moe.styx.styx2m.mpvkit.events", qos: .userInitiated)

    override func create(
        videoOutputDriver: String,
        gpuApi: String,
        profile: String,
        hwdec: String,
        deband: Bool,
        debandIterations: String,
        dither10bit: Bool,
        blendSubtitles: Bool,
        slang: String,
        alang: String
    ) {
        guard mpv == nil else { return }
        guard let ctx = mpv_create() else {
            print("[MPVKit] mpv_create failed")
            return
        }
        mpv = ctx

        let usesVulkan = gpuApi == "vulkan"
        setOption(ctx, "config", "yes")
        setOption(ctx, "profile", profile)
        if profile == "default" {
            setOption(ctx, "scale", "catmull_rom")
            setOption(ctx, "cscale", "bilinear")
            setOption(ctx, "dscale", "bilinear")
        }
        setOption(ctx, "dither", profile == "high-quality" ? "fruit" : "ordered")
        setOption(ctx, "hwdec", hwdec)
        setOption(ctx, "tls-verify", "no")
        setOption(ctx, "blend-subtitles", blendSubtitles ? "yes" : "no")
        setOption(ctx, "cache", "yes")
        setOption(ctx, "demuxer-max-bytes", "50MiB")
        setOption(ctx, "demuxer-max-back-bytes", "32MiB")
        setOption(ctx, "force-window", "no")
        setOption(ctx, "keep-open", "always")
        setOption(ctx, "idle", "yes")
        setOption(ctx, "input-default-bindings", "no")
        setOption(ctx, "input-vo-keyboard", "no")
        setOption(ctx, "save-position-on-quit", "no")
        setOption(ctx, "sub-font-provider", "none")
        setOption(ctx, "hwdec-codecs", "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1")
        setOption(ctx, "deband", deband ? "yes" : "no")
        setOption(ctx, "deband-iterations", debandIterations)
        setOption(ctx, "dither-depth", dither10bit ? "10" : "8")
        setOption(ctx, "slang", slang)
        setOption(ctx, "alang", alang)

        if usesVulkan {
            setOption(ctx, "vo", videoOutputDriver)
            setOption(ctx, "gpu-api", "vulkan")
            setOption(ctx, "gpu-context", "moltenvk")

            if let view = renderView {
                setWidLayer(ctx, view.metalLayer)
            } else {
                print("[MPVKit] WARNING: create() called before getPlayerView(); video will not render")
            }
        } else {
            setOption(ctx, "vo", "libmpv")
        }

        if mpv_initialize(ctx) < 0 {
            print("[MPVKit] mpv_initialize failed")
            mpv_destroy(ctx)
            mpv = nil
            return
        }

        if !usesVulkan {
            setupOpenGLRenderContext(ctx)
        }

        observe("time-pos", MPV_FORMAT_DOUBLE)
        observe("duration", MPV_FORMAT_DOUBLE)
        observe("demuxer-cache-time", MPV_FORMAT_DOUBLE)
        observe("percent-pos", MPV_FORMAT_DOUBLE)
        observe("pause", MPV_FORMAT_FLAG)
        observe("paused-for-cache", MPV_FORMAT_FLAG)
        observe("seeking", MPV_FORMAT_FLAG)
        observe("metadata/by-key/title", MPV_FORMAT_STRING)

        let opaqueSelf = Unmanaged.passUnretained(self).toOpaque()
        mpv_set_wakeup_callback(ctx, { ctxPtr in
            guard let ctxPtr else { return }
            let bridge = Unmanaged<MpvKitBridgeImpl>.fromOpaque(ctxPtr).takeUnretainedValue()
            bridge.scheduleEventDrain()
        }, opaqueSelf)

        scheduleEventDrain()
    }

    override func destroy() {
        guard let ctx = mpv else { return }
        mpv_set_wakeup_callback(ctx, nil, nil)
        eventQueue.sync {}

        onPropertyChange = nil
        onEvent = nil

        if let mpvGL {
            mpv_render_context_free(mpvGL)
            self.mpvGL = nil
        }
        mpv_terminate_destroy(ctx)
        mpv = nil
        renderView = nil
        openGLView = nil
        hasLoadedFile = false
        lastSurfaceSize = .zero
    }

    override func getPlayerView(gpuApi: String) -> UIView {
        if gpuApi == "vulkan" {
            if let renderView {
                return renderView
            }

            let view = MPVRenderView(frame: .zero)
            renderView = view
            return view
        }

        if let openGLView {
            return openGLView
        }

        let view = MPVOpenGLRenderView()
        view.bridge = self
        openGLView = view
        return view
    }

    override func resizeSurface(width: Int32, height: Int32) {
        guard width > 1, height > 1 else { return }

        DispatchQueue.main.async { [weak self] in
            guard let view = self?.renderView else { return }
            view.setNeedsLayout()
            view.layoutIfNeeded()
        }

        let surfaceSize = CGSize(width: Int(width), height: Int(height))
        guard surfaceSize != lastSurfaceSize else { return }

        lastSurfaceSize = surfaceSize

        printMpvRenderSize(context: "surface resize \(Int(width))x\(Int(height))")
    }

    override func loadFile(path: String, startAt: Double) {
        load(pathOrUrl: path, startAt: startAt)
    }

    override func loadUrl(url: String, startAt: Double) {
        load(pathOrUrl: url, startAt: startAt)
    }

    override func setPaused(paused: Bool) {
        guard let ctx = mpv else { return }
        var flag: Int32 = paused ? 1 : 0
        mpv_set_property(ctx, "pause", MPV_FORMAT_FLAG, &flag)
    }

    override func seekTo(positionSeconds: Double) {
        guard let ctx = mpv else { return }
        var position = positionSeconds
        mpv_set_property(ctx, "time-pos", MPV_FORMAT_DOUBLE, &position)
    }

    override func setAudioTrack(id: Int32) {
        guard let ctx = mpv else { return }
        if id < 0 {
            mpv_set_property_string(ctx, "aid", "no")
        } else {
            var value = Int64(id)
            mpv_set_property(ctx, "aid", MPV_FORMAT_INT64, &value)
        }
    }

    override func setSubtitleTrack(id: Int32) {
        guard let ctx = mpv else { return }
        if id < 0 {
            mpv_set_property_string(ctx, "sid", "no")
        } else {
            var value = Int64(id)
            mpv_set_property(ctx, "sid", MPV_FORMAT_INT64, &value)
        }
    }

    override func showMessage(message: String, durationMillis: Int32) {
        command(["show-text", message, "\(durationMillis)"])
    }

    override func getTrackCount() -> Int32 {
        getPropertyInt64("track-list/count").map(Int32.init) ?? 0
    }

    override func getTrackType(index: Int32) -> String? {
        getPropertyString("track-list/\(index)/type")
    }

    override func getTrackId(index: Int32) -> Int32 {
        getPropertyInt64("track-list/\(index)/id").map(Int32.init) ?? 0
    }

    override func getTrackLang(index: Int32) -> String? {
        getPropertyString("track-list/\(index)/lang")
    }

    override func getTrackTitle(index: Int32) -> String? {
        getPropertyString("track-list/\(index)/title")
    }

    override func getTrackCodec(index: Int32) -> String? {
        getPropertyString("track-list/\(index)/codec")
    }

    override func isTrackDefault(index: Int32) -> Bool {
        getPropertyFlag("track-list/\(index)/default") ?? false
    }

    override func isTrackForced(index: Int32) -> Bool {
        getPropertyFlag("track-list/\(index)/forced") ?? false
    }

    override func isTrackSelected(index: Int32) -> Bool {
        getPropertyFlag("track-list/\(index)/selected") ?? false
    }

    override func getChapterCount() -> Int32 {
        getPropertyInt64("chapter-list/count").map(Int32.init) ?? 0
    }

    override func getChapterTitle(index: Int32) -> String? {
        getPropertyString("chapter-list/\(index)/title")
    }

    override func getChapterTime(index: Int32) -> Double {
        getPropertyDouble("chapter-list/\(index)/time") ?? 0
    }

    private func load(pathOrUrl: String, startAt: Double) {
        hasLoadedFile = false
        command(["set", "start", "\(startAt)"])
        command(["loadfile", pathOrUrl, "replace"])
    }

    private func setupOpenGLRenderContext(_ ctx: OpaquePointer) {
        guard let view = openGLView else {
            print("[MPVKit] WARNING: create() called before OpenGL view exists; video will not render")
            return
        }

        EAGLContext.setCurrent(view.context)

        let api = UnsafeMutableRawPointer(mutating: (MPV_RENDER_API_TYPE_OPENGL as NSString).utf8String)
        var initParams = mpv_opengl_init_params(
            get_proc_address: { _, name in
                return MpvKitBridgeImpl.getOpenGLProcAddress(name)
            },
            get_proc_address_ctx: nil
        )

        withUnsafeMutablePointer(to: &initParams) { initParams in
            var params = [
                mpv_render_param(type: MPV_RENDER_PARAM_API_TYPE, data: api),
                mpv_render_param(type: MPV_RENDER_PARAM_OPENGL_INIT_PARAMS, data: initParams),
                mpv_render_param()
            ]

            if mpv_render_context_create(&mpvGL, ctx, &params) < 0 {
                print("[MPVKit] failed to initialize OpenGL render context")
                return
            }
        }

        mpv_render_context_set_update_callback(
            mpvGL,
            { context in
                guard let context else { return }
                let view = Unmanaged<MPVOpenGLRenderView>.fromOpaque(context).takeUnretainedValue()
                DispatchQueue.main.async {
                    view.display()
                }
            },
            Unmanaged.passUnretained(view).toOpaque()
        )
    }

    func renderOpenGLFrame(in view: GLKView) {
        guard let mpvGL else { return }
        EAGLContext.setCurrent(view.context)

        glClearColor(0, 0, 0, 0)
        glClear(GLbitfield(GL_COLOR_BUFFER_BIT))

        var framebuffer: GLint = 0
        glGetIntegerv(GLenum(GL_FRAMEBUFFER_BINDING), &framebuffer)

        var dims: [GLint] = [0, 0, 0, 0]
        glGetIntegerv(GLenum(GL_VIEWPORT), &dims)

        var framebufferData = mpv_opengl_fbo(
            fbo: Int32(framebuffer),
            w: Int32(dims[2]),
            h: Int32(dims[3]),
            internal_format: 0
        )

        var flip: CInt = 1
        withUnsafeMutablePointer(to: &flip) { flip in
            withUnsafeMutablePointer(to: &framebufferData) { framebufferData in
                var params = [
                    mpv_render_param(type: MPV_RENDER_PARAM_OPENGL_FBO, data: framebufferData),
                    mpv_render_param(type: MPV_RENDER_PARAM_FLIP_Y, data: flip),
                    mpv_render_param()
                ]
                mpv_render_context_render(mpvGL, &params)
            }
        }
    }

    private static func getOpenGLProcAddress(_ name: UnsafePointer<CChar>?) -> UnsafeMutableRawPointer? {
        let symbolName = CFStringCreateWithCString(kCFAllocatorDefault, name, CFStringBuiltInEncodings.ASCII.rawValue)
        let identifier = CFBundleGetBundleWithIdentifier("com.apple.opengles" as CFString)
        return CFBundleGetFunctionPointerForName(identifier, symbolName)
    }

    private func observe(_ name: String, _ format: mpv_format) {
        guard let ctx = mpv else { return }
        check(mpv_observe_property(ctx, 0, name, format), "observe \(name)")
    }

    private func scheduleEventDrain() {
        eventQueue.async { [weak self] in
            guard let self else { return }
            while let ctx = self.mpv {
                let event = mpv_wait_event(ctx, 0)
                guard let pointee = event?.pointee else { return }
                if pointee.event_id == MPV_EVENT_NONE { return }
                self.handle(event: pointee)
            }
        }
    }

    private func handle(event: mpv_event) {
        switch event.event_id {
        case MPV_EVENT_PROPERTY_CHANGE:
            guard let property = event.data?
                .assumingMemoryBound(to: mpv_event_property.self)
                .pointee
            else { return }
            let name = String(cString: property.name)
            let value = propertyValue(property)
            DispatchQueue.main.async { [weak self] in
                self?.onPropertyChange?(name, value)
            }
        case MPV_EVENT_FILE_LOADED:
            DispatchQueue.main.async { [weak self] in
                self?.hasLoadedFile = true
                self?.printMpvRenderSize(context: "file loaded")
                self?.onEvent?("file-loaded")
            }
        case MPV_EVENT_END_FILE:
            DispatchQueue.main.async { [weak self] in self?.onEvent?("end-file") }
        default:
            break
        }
    }

    private func propertyValue(_ property: mpv_event_property) -> Any? {
        guard let data = property.data else { return nil }

        switch property.format {
        case MPV_FORMAT_DOUBLE:
            return data.assumingMemoryBound(to: Double.self).pointee
        case MPV_FORMAT_FLAG:
            return data.assumingMemoryBound(to: Int32.self).pointee != 0
        case MPV_FORMAT_INT64:
            return data.assumingMemoryBound(to: Int64.self).pointee
        case MPV_FORMAT_STRING:
            let cString = data.assumingMemoryBound(to: UnsafePointer<CChar>.self).pointee
            return String(cString: cString)
        default:
            return nil
        }
    }

    @discardableResult
    private func command(_ args: [String]) -> Int32 {
        guard let ctx = mpv else { return -1 }
        var cargs: [UnsafeMutablePointer<CChar>?] = args.map { strdup($0) }
        cargs.append(nil)

        let result: Int32 = cargs.withUnsafeMutableBufferPointer { buffer in
            buffer.baseAddress?.withMemoryRebound(
                to: UnsafePointer<CChar>?.self,
                capacity: buffer.count
            ) { ptr in
                mpv_command(ctx, ptr)
            } ?? -1
        }

        for ptr in cargs {
            free(ptr)
        }

        if result < 0 {
            print("[MPVKit] command \(args) failed: \(String(cString: mpv_error_string(result)))")
        }
        return result
    }

    private func getPropertyString(_ name: String) -> String? {
        guard let ctx = mpv else { return nil }
        guard let cString = mpv_get_property_string(ctx, name) else { return nil }
        let value = String(cString: cString)
        mpv_free(cString)
        return value
    }

    private func getPropertyDouble(_ name: String) -> Double? {
        guard let ctx = mpv else { return nil }
        var value: Double = 0
        return mpv_get_property(ctx, name, MPV_FORMAT_DOUBLE, &value) < 0 ? nil : value
    }

    private func getPropertyInt64(_ name: String) -> Int64? {
        guard let ctx = mpv else { return nil }
        var value: Int64 = 0
        return mpv_get_property(ctx, name, MPV_FORMAT_INT64, &value) < 0 ? nil : value
    }

    private func getPropertyFlag(_ name: String) -> Bool? {
        guard let ctx = mpv else { return nil }
        var value: Int32 = 0
        return mpv_get_property(ctx, name, MPV_FORMAT_FLAG, &value) < 0 ? nil : value != 0
    }

    private func printMpvRenderSize(context: String) {
        guard mpv != nil else { return }
        let osdWidth = getPropertyInt64("osd-width")
        let osdHeight = getPropertyInt64("osd-height")
        let displayWidth = getPropertyInt64("display-width")
        let displayHeight = getPropertyInt64("display-height")
        print("[MPVKit] \(context) mpv osd=\(optionalSize(osdWidth, osdHeight)) display=\(optionalSize(displayWidth, displayHeight))")
    }

    private func optionalSize(_ width: Int64?, _ height: Int64?) -> String {
        guard let width, let height else { return "unavailable" }
        return "\(width)x\(height)"
    }

    private func setOption(_ ctx: OpaquePointer, _ name: String, _ value: String) {
        check(mpv_set_option_string(ctx, name, value), "set option \(name)=\(value)")
    }

    private func setWidLayer(_ ctx: OpaquePointer, _ layer: CAMetalLayer) {
        var widLayer = layer
        check(mpv_set_option(ctx, "wid", MPV_FORMAT_INT64, &widLayer), "set option wid")
    }

    @discardableResult
    private func check(_ status: Int32, _ context: String) -> Int32 {
        if status < 0 {
            print("[MPVKit] \(context) failed: \(String(cString: mpv_error_string(status)))")
        }
        return status
    }
}
