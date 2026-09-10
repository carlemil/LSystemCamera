package se.kjellstrand.lsystemcamera

import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import platform.UIKit.UIViewController
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel

/** Entry point for the Xcode app: wrap this in a UIViewControllerRepresentable. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    LSystemTheme {
        MainScreen(viewModel { LSystemViewModel() })
    }
}
