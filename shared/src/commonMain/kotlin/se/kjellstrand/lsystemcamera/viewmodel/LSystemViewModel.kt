package se.kjellstrand.lsystemcamera.viewmodel

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.lsystemcamera.ImageAnalyzer
import se.kjellstrand.lsystemcamera.LumaFrame

data class UiState(
    val system: LSystem = LSystem.getByName("Moore"),
    val iterations: Int = system.defaultIterations,
    val contrast: Float = 1f,
    val brightness: Float = 0f,
)

class LSystemViewModel : ViewModel() {

    val ui = MutableStateFlow(UiState())
    val frame = MutableStateFlow<ImageBitmap?>(null)
    /** Read by the camera source, which rebinds when it flips. */
    val frontCamera = MutableStateFlow(false)
    /** Shutter: while true the last frame is kept and new camera images are dropped. */
    val frozen = MutableStateFlow(false)

    /** Lives here, not in the composition, so it survives recreation on theme change. */
    private val renderer = ImageAnalyzer()

    /**
     * Called from the camera's delivery thread, one frame at a time — that serialisation is the
     * whole thread model, so the renderer needs no executor of its own.
     */
    fun onFrame(image: LumaFrame) {
        if (!frozen.value) frame.value = renderer.analyze(image, ui.value)
    }

    fun select(system: LSystem) = ui.update { UiState(system = system, contrast = it.contrast, brightness = it.brightness) }
}
