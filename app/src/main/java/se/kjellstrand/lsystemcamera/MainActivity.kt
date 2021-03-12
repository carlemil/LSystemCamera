package se.kjellstrand.lsystemcamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
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
import com.google.android.material.slider.Slider
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.single_line_text_item.view.*
import se.kjellstrand.lsystem.LSystemGenerator
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.lsystemcamera.view.CustomAdapter
import se.kjellstrand.lsystemcamera.view.RowItem
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraXBasic"
        private const val CAMERA_IMAGE_SIZE = 200
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val DEFAULT_SYSTEM = "Moore"

        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var cameraExecutor: ExecutorService
    private val model: LSystemViewModel by viewModels()
    private val analyzerExecutor = Executors.newSingleThreadExecutor()
    private val bannedSystemNames = listOf("KochSnowFlake")

    private var bitmap: Bitmap? = null

    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupLSystemObserver()
        setupMaxIterationsObserver()

        requestCameraPermissions()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun requestCameraPermissions() {
        if (allPermissionsGranted()) {
            startCamera(CAMERA_IMAGE_SIZE)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun setupLSystemObserver() {
        model.observeLSystem(this, {
            ImageAnalyzer.updateLSystem(model)
            model.getLSystem()?.let { system ->
                model.calculateAndSetMaxIterations(system, imageView)

                val (minWidth, maxWidth) = LSystemGenerator.getRecommendedMinAndMaxWidth(
                    1f, model.getIterations(), system
                )
                model.setMinWidth(minWidth)
                model.setMaxWidth(maxWidth)

                val maxIterations = model.getMaxIterations()
                val iterations = (maxIterations / 2).toFloat()
                val iterationsSlider: Slider = findViewById(R.id.iterationsSlider)
                iterationsSlider.value = 2f
                iterationsSlider.valueTo = maxIterations.toFloat()
                model.setIterations(iterations.toInt())
                iterationsSlider.value = iterations
            }
        })
    }

    private fun setupMaxIterationsObserver() {
        model.observeMaxIterations(this, { maxIterations ->
            model.getLSystem()?.let { system ->
                val (minWidth, maxWidth) = LSystemGenerator.getRecommendedMinAndMaxWidth(
                    1f, maxIterations, system
                )
                model.setMinWidth(minWidth)
                model.setMaxWidth(maxWidth)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        inflateSystemNameSpinner()
        inflateBrightnessAndContrastSliders()
        inflateIterationsSlider()
    }

    private fun inflateSystemNameSpinner() {
        val systemsNames = LSystem.systems
            .map { system -> system.name }
            .sorted()
            .filter { name -> name !in bannedSystemNames }

        val spinner: Spinner = findViewById(R.id.system_selector_spinner)
        spinner.adapter = CustomAdapter(
            this@MainActivity,
            R.layout.single_line_text_item,
            R.id.title,
            systemsNames.map { name -> RowItem(name) }
        )
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View,
                position: Int,
                id: Long
            ) {
                LSystem.getByName(systemsNames[position])
                    .let { system -> model.setLSystem(system) }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }

        val selectedIndex = systemsNames.indexOf(DEFAULT_SYSTEM)
        if (selectedIndex != -1) {
            spinner.setSelection(selectedIndex)
        }
    }

    private fun inflateBrightnessAndContrastSliders() {
        val contrastSlider: Slider = findViewById(R.id.contrastSlider)
        contrastSlider.addOnChangeListener { slider, _, _ ->
            model.setContrastMod(slider.value)
        }
        contrastSlider.setLabelFormatter { value -> value.toString() }

        val brightnessSlider: Slider = findViewById(R.id.brightnessSlider)
        brightnessSlider.addOnChangeListener { slider, _, _ ->
            model.setBrightnessMod(slider.value)
        }
        brightnessSlider.setLabelFormatter { value -> value.toString() }
    }

    private fun inflateIterationsSlider() {
        val iterationsSlider: Slider = findViewById(R.id.iterationsSlider)
        iterationsSlider.addOnChangeListener { slider, _, _ ->
            model.setIterations(slider.value.toInt())
        }
        contrastSlider.setLabelFormatter { value -> value.toString() }
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
}