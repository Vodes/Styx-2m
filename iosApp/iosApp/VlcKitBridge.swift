import Foundation
import UIKit
import MobileVLCKit
import ComposeApp

private struct VlcTrack {
    let id: Int32
    let type: String
    let title: String?
    let language: String?
    let codec: String?
    let selected: Bool
}

private struct VlcTrackKey: Hashable {
    let id: Int32
    let type: String
}

private struct VlcTrackMetadata {
    let language: String?
    let codec: String?
}

final class VlcRenderView: UIView {
    override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .black
        isUserInteractionEnabled = false
        clipsToBounds = true
        autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("VlcRenderView is code-instantiated only")
    }
}

final class VlcKitBridgeImpl: VlcKitPlayerBridge, VLCMediaPlayerDelegate {
    private let player = VLCMediaPlayer()
    private let renderView = VlcRenderView()
    private var slang = ""
    private var alang = ""
    private var tracks: [VlcTrack] = []

    override func create(slang: String, alang: String) {
        self.slang = slang
        self.alang = alang
        player.delegate = self
        player.drawable = renderView
    }

    override func destroy() {
        player.delegate = nil
        player.stop()
        player.drawable = nil
        player.media = nil
        tracks.removeAll()
        onPropertyChange = nil
        onEvent = nil
    }

    override func getPlayerView() -> UIView {
        renderView
    }

    override func resizeSurface(width: Int32, height: Int32) {
        // Intentionally unused for now. Let UIKit/VLCKit own layout.
    }

    override func loadFile(path: String, startAt: Double) {
        load(media: VLCMedia(path: path), startAt: startAt)
    }

    override func loadUrl(url: String, startAt: Double) {
        guard let mediaUrl = URL(string: url) else {
            emitEvent("error")
            return
        }
        load(media: VLCMedia(url: mediaUrl), startAt: startAt)
    }

    override func setPaused(paused: Bool) {
        if paused {
            player.pause()
        } else {
            player.play()
        }
        emitProperty("pause", paused)
    }

    override func seekTo(positionSeconds: Double) {
        player.time = VLCTime(int: Int32(positionSeconds * 1000.0))
    }

    override func setAudioTrack(id: Int32) {
        player.currentAudioTrackIndex = id
        refreshTracks()
        emitEvent("tracks")
    }

    override func setSubtitleTrack(id: Int32) {
        player.currentVideoSubTitleIndex = id
        refreshTracks()
        emitEvent("tracks")
    }

    override func showMessage(message: String, durationMillis: Int32) {
        // The Compose controls own user-visible messages for now.
    }

    override func getTrackCount() -> Int32 {
        Int32(tracks.count)
    }

    override func getTrackType(index: Int32) -> String? {
        track(at: index)?.type
    }

    override func getTrackId(index: Int32) -> Int32 {
        track(at: index)?.id ?? -1
    }

    override func getTrackLang(index: Int32) -> String? {
        track(at: index)?.language
    }

    override func getTrackTitle(index: Int32) -> String? {
        track(at: index)?.title
    }

    override func getTrackCodec(index: Int32) -> String? {
        track(at: index)?.codec
    }

    override func isTrackDefault(index: Int32) -> Bool {
        false
    }

    override func isTrackForced(index: Int32) -> Bool {
        false
    }

    override func isTrackSelected(index: Int32) -> Bool {
        track(at: index)?.selected ?? false
    }

    override func getChapterCount() -> Int32 {
        0
    }

    override func getChapterTitle(index: Int32) -> String? {
        nil
    }

    override func getChapterTime(index: Int32) -> Double {
        0.0
    }

    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        let currentSeconds = Double(player.time.intValue) / 1000.0
        emitProperty("time-pos", currentSeconds)

        let lengthMillis = player.media?.length.intValue ?? 0
        if lengthMillis > 0 {
            let durationSeconds = Double(lengthMillis) / 1000.0
            emitProperty("duration", durationSeconds)
        }
    }

    func mediaPlayerStateChanged(_ aNotification: Notification) {
        switch player.state {
        case .opening:
            emitEvent("opening")
        case .buffering:
            emitEvent("buffering")
        case .playing:
            emitProperty("pause", false)
            refreshTracks()
            emitEvent("playing")
        case .paused:
            emitProperty("pause", true)
            emitEvent("paused")
        case .stopped:
            emitEvent("stopped")
        case .ended:
            emitEvent("end-file")
        case .error:
            emitEvent("error")
        case .esAdded:
            refreshTracks()
            emitEvent("tracks")
        default:
            break
        }
    }

    private func load(media: VLCMedia, startAt: Double) {
        var options: [String: Any] = [
            "network-caching": 1000,
            "clock-jitter": 0,
        ]
        if startAt > 0 {
            options["start-time"] = startAt
        }
        if !slang.isBlank {
            options["sub-language"] = slang
        }
        if !alang.isBlank {
            options["audio-language"] = alang
        }
        media.addOptions(options)

        player.media = media
        player.play()
        if startAt > 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) { [weak self] in
                self?.seekTo(positionSeconds: startAt)
            }
        }
    }

    private func refreshTracks() {
        let metadata = buildTrackMetadata()
        var updated: [VlcTrack] = []
        updated.append(contentsOf: buildTracks(
            ids: player.audioTrackIndexes,
            names: player.audioTrackNames,
            type: "audio",
            selectedId: player.currentAudioTrackIndex,
            metadata: metadata
        ))
        updated.append(contentsOf: buildTracks(
            ids: player.videoSubTitlesIndexes,
            names: player.videoSubTitlesNames,
            type: "sub",
            selectedId: player.currentVideoSubTitleIndex,
            metadata: metadata
        ))

        tracks = updated
    }

    private func buildTracks(
        ids: [Any]?,
        names: [Any]?,
        type: String,
        selectedId: Int32,
        metadata: [VlcTrackKey: VlcTrackMetadata]
    ) -> [VlcTrack] {
        let trackIds = ids ?? []
        let trackNames = names ?? []

        return trackIds.enumerated().compactMap { offset, rawId in
            guard let id = rawId as? NSNumber else { return nil }
            guard id.int32Value >= 0 else { return nil }
            let title = offset < trackNames.count ? trackNames[offset] as? String : nil
            let trackMetadata = metadata[VlcTrackKey(id: id.int32Value, type: type)]
            return VlcTrack(
                id: id.int32Value,
                type: type,
                title: title,
                language: trackMetadata?.language,
                codec: trackMetadata?.codec,
                selected: id.int32Value == selectedId
            )
        }
    }

    private func buildTrackMetadata() -> [VlcTrackKey: VlcTrackMetadata] {
        guard let trackInfo = player.media?.tracksInformation as? [[AnyHashable: Any]] else {
            return [:]
        }

        var metadata: [VlcTrackKey: VlcTrackMetadata] = [:]
        for info in trackInfo {
            guard
                let id = info[VLCMediaTracksInformationId] as? NSNumber,
                let rawType = info[VLCMediaTracksInformationType] as? String,
                let type = appTrackType(from: rawType)
            else {
                continue
            }

            metadata[VlcTrackKey(id: id.int32Value, type: type)] = VlcTrackMetadata(
                language: nonBlankString(info[VLCMediaTracksInformationLanguage]),
                codec: codecString(info[VLCMediaTracksInformationCodec])
            )
        }
        return metadata
    }

    private func appTrackType(from vlcType: String) -> String? {
        switch vlcType {
        case VLCMediaTracksInformationTypeAudio:
            return "audio"
        case VLCMediaTracksInformationTypeText:
            return "sub"
        default:
            return nil
        }
    }

    private func nonBlankString(_ value: Any?) -> String? {
        guard let string = value as? String else { return nil }
        let trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }

    private func codecString(_ value: Any?) -> String? {
        if let string = nonBlankString(value) {
            return string
        }
        if let number = value as? NSNumber {
            return number.stringValue
        }
        return nil
    }

    private func track(at index: Int32) -> VlcTrack? {
        let offset = Int(index)
        guard offset >= 0 && offset < tracks.count else { return nil }
        return tracks[offset]
    }

    private func emitProperty(_ name: String, _ value: Any?) {
        DispatchQueue.main.async { [weak self] in
            self?.onPropertyChange?(name, value)
        }
    }

    private func emitEvent(_ name: String) {
        DispatchQueue.main.async { [weak self] in
            self?.onEvent?(name)
        }
    }
}

private extension String {
    var isBlank: Bool {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
