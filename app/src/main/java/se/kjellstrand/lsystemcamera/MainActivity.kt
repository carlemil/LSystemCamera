package se.kjellstrand.lsystemcamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.chip.Chip
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val CAMERA_IMAGE_SIZE = 200

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService

    private val model: LSystemViewModel by viewModels()
    private val analyzerExecutor = Executors.newSingleThreadExecutor()
    private val bannedSystemNames = listOf("KochSnowFlake")

    private var bitmap: Bitmap? = null

    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inflateSystemNameSelectorChips()

        model.lSystem.observe(this, Observer {
            ImageAnalyzer.updateLSystem(model, imageView)
        })

        // Set a random system to start with.
        val defaultSystem = LSystem.getByName("Moore")
        defaultSystem?.let { system ->
            model.lSystem.value =
                system // LSystem.systems[Random.nextInt(0, LSystem.systems.size - 1)].name)
            ImageAnalyzer.updateLSystem(model, imageView)
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera(CAMERA_IMAGE_SIZE)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun inflateSystemNameSelectorChips() {
        LSystem.systems
            .map { system -> system.name }
            .filter { it !in bannedSystemNames }
            .sorted()
            .forEach { name ->
                val chip = Chip(this)
                chip.text = name
                chip.setOnClickListener {
                    LSystem.getByName(name).let { system -> model.lSystem.value = system }
                }
                chips.addView(chip)
            }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @androidx.camera.core.ExperimentalGetImage
    private fun startCamera(size: Int) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            handleCamera(cameraProviderFuture, size)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleCamera(
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
        size: Int
    ) {
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Preview
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(viewFinder.surfaceProvider)
        }

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(size, size))
            .setTargetRotation(Surface.ROTATION_0) // TODO Figure out why this is broken and do not work.
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(analyzerExecutor, { image ->
            bitmap = ImageAnalyzer.analyzeImage(image, imageView, model)
            runOnUiThread {
                imageView.setImageBitmap(bitmap)
            }
        })

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(CAMERA_IMAGE_SIZE)
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}