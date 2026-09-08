package se.kjellstrand.lsystemcamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: LSystemViewModel by viewModels()
    private var hasCamera by mutableStateOf(false)
    private var bound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LSystemTheme { MainScreen(vm, hasCamera, onShare = ::share) } }
    }

    // Covers first launch, returning from the permission dialog, and returning from Settings.
    override fun onResume() {
        super.onResume()
        hasCamera = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasCamera) bindCamera()
    }

    private fun bindCamera() {
        if (bound) return
        bound = true
        lifecycleScope.launch {
            val provider = ProcessCameraProvider.awaitInstance(this@MainActivity)
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
            analysis.setAnalyzer(vm.executor, vm.analyzer)
            vm.frontCamera.collect { front ->
                val selector = if (front && provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                provider.unbindAll()
                provider.bindToLifecycle(this@MainActivity, selector, analysis)
            }
        }
    }

    private fun share() {
        val bitmap = vm.frame.value ?: return
        val file = File(cacheDir, "imageview").apply { mkdirs() }.resolve("image.png")
        file.outputStream().use { Bitmap.createBitmap(bitmap).compress(Bitmap.CompressFormat.PNG, 100, it) }
        ShareCompat.IntentBuilder(this)
            .setType("image/png")
            .setStream(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
            .startChooser()
    }

    private companion object {
        const val ANALYSIS_SIZE = 200
    }
}
