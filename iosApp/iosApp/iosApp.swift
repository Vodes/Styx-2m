import UIKit
import SwiftUI
import ComposeApp

@main
struct iosApp: App {
    init() {
        VlcKitBridgeKt.instantiateVlcKitBridge = {
            VlcKitBridgeImpl()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
            .persistentSystemOverlays(.visible)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        ComposeContainerViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

final class ComposeContainerViewController: UIViewController {
    private let composeViewController = MainKt.MainViewController()

    override var prefersStatusBarHidden: Bool { false }
    override var childForStatusBarHidden: UIViewController? { nil }
    override var prefersHomeIndicatorAutoHidden: Bool { false }
    override var childForHomeIndicatorAutoHidden: UIViewController? { nil }
    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge { [] }
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        UserDefaults.standard.bool(forKey: "styx.forceLandscape") ? .landscape : .allButUpsideDown
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(orientationLockChanged),
            name: Notification.Name("styx.orientationLockChanged"),
            object: nil
        )

        addChild(composeViewController)
        view.addSubview(composeViewController.view)
        composeViewController.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            composeViewController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            composeViewController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            composeViewController.view.topAnchor.constraint(equalTo: view.topAnchor),
            composeViewController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        composeViewController.didMove(toParent: self)

        applyOrientationLock()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        applyOrientationLock()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    @objc private func orientationLockChanged() {
        applyOrientationLock()
    }

    private func applyOrientationLock() {
        let orientationMask: UIInterfaceOrientationMask =
            UserDefaults.standard.bool(forKey: "styx.forceLandscape") ? .landscape : .allButUpsideDown

        setNeedsUpdateOfSupportedInterfaceOrientations()

        guard let windowScene = view.window?.windowScene else { return }

        if #available(iOS 16.0, *) {
            windowScene.requestGeometryUpdate(.iOS(interfaceOrientations: orientationMask)) { error in
                print("[Orientation] requestGeometryUpdate failed: \(error.localizedDescription)")
            }
        }
    }
}
