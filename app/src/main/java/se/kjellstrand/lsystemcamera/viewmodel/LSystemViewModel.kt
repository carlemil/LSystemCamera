package se.kjellstrand.lsystemcamera.viewmodel

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.lsystemcamera.ImageAnalyzer
import java.util.concurrent.Executors

data class UiState(
    val system: LSystem = LSystem.getByName("Moore"),
    val iterations: Int = system.maxIterations - 1,
    val contrast: Float = 1f,
    val brightness: Float = 0f,
)

class LSystemViewModel : ViewModel() {

    val ui = MutableStateFlow(UiState())
    val frame = MutableStateFlow<Bitmap?>(null)
    /** Observed by MainActivity, which rebinds the camera when it flips. */
    val frontCamera = MutableStateFlow(false)

    /** Lives here, not in the Activity, so it survives recreation on theme change. */
    val executor = Executors.newSingleThreadExecutor()
    private val renderer = ImageAnalyzer()
    val analyzer = ImageAnalysis.Analyzer { image ->
        image.use { frame.value = renderer.analyze(it, ui.value) }
    }

    fun select(system: LSystem) = ui.update { UiState(system = system, contrast = it.contrast, brightness = it.brightness) }

    override fun onCleared() = executor.shutdown()
}
