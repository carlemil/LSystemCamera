package se.kjellstrand.lsystemcamera

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

/** One camera frame, over `ImageProxy.planes[0]`. Valid only until the proxy is closed. */
private class ImageProxyFrame(image: ImageProxy) : LumaFrame {
    private val plane = image.planes[0]
    private val buffer = plane.buffer
    override val width = image.width
    override val height = image.height
    override val rowStride = plane.rowStride
    override val pixelStride = plane.pixelStride
    override val rotationDegrees = image.imageInfo.rotationDegrees
    override fun byteAt(offset: Int) = buffer.get(offset).toInt() and 0xFF
}

@Composable
actual fun rememberCameraSource(front: Boolean, onFrame: (LumaFrame) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame by rememberUpdatedState(onFrame)

    // Analysis runs off the main thread; KEEP_ONLY_LATEST plus a single thread is the backpressure.
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(executor) { onDispose { executor.shutdown() } }

    val provider by produceState<ProcessCameraProvider?>(null, context) {
        value = ProcessCameraProvider.awaitInstance(context)
    }

    DisposableEffect(provider, lifecycleOwner, front) {
        val cameraProvider = provider
        if (cameraProvider != null) {
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(ANALYSIS_SIZE, ANALYSIS_SIZE),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { image ->
                image.use { currentOnFrame(ImageProxyFrame(it)) }
            }
            val selector = if (front && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, analysis)
        }
        onDispose { cameraProvider?.unbindAll() }
    }
}

private const val ANALYSIS_SIZE = 200
