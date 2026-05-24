import UIKit
import SwiftUI
import ComposeApp

@main
struct iosApp: App {
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

    override func viewDidLoad() {
        super.viewDidLoad()

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
    }
}
