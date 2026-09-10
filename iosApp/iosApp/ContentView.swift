import SwiftUI
import UIKit
import Shared

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea()
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // MainViewControllerKt is the auto-generated ObjC class name for
        // top-level `fun MainViewController()` in shared/iosMain.
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
