package se.kjellstrand.lsystemcamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import se.kjellstrand.lsystem.LSystemGenerator
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.variablewidthline.LinePoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val CAMERA_IMAGE_SIZE = 100
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private val analyzerExecutor = Executors.newSingleThreadExecutor()

    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera(CAMERA_IMAGE_SIZE)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listener for take photo button
        camera_capture_button.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
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
            it.setSurfaceProvider(viewFinder.createSurfaceProvider())
        }

        imageCapture = ImageCapture.Builder().build()

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(size, size))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(analyzerExecutor, { image ->
            analyzeImage(image, size)
        })

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun analyzeImage(image: ImageProxy, size: Int) {
        val rotationDegrees = image.imageInfo.rotationDegrees

        image.setCropRect(Rect(0, 0, size, size))

        val luminance: Array<ByteArray> = Array(image.width) { ByteArray(image.height) }

        val plane = image.image?.planes?.get(0)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val byte = plane?.buffer?.get(x + y * image.width) ?: Byte.MIN_VALUE
                luminance[x][y] = byte
            }
        }

        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)

        var min = Int.MAX_VALUE
        var max = Int.MIN_VALUE
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val color = getColorFromLuminanceValue(luminance[x][y])
                bitmap.setPixel(x, y, color)
                if (min > color) min = color
                if (max < color) max = color

            }
        }
        LSystem.getByName("Hilbert")?.let { lSystem ->
            val line = LSystemGenerator.generatePolygon(lSystem, 3)
            val vwLine = line.map { linePoint -> LinePoint(linePoint.x, linePoint.y, 1.0) }
            println("vwLine: ${vwLine.size}")
        }


        runOnUiThread {
            imageView.setImageBitmap(bitmap)
        }

        image.close()
    }

    private fun getColorFromLuminanceValue(byte: Byte): Int {
        val i = getByteValueFromLuminanceValue(byte).toInt()
        return ((i and 0xFF) + ((i shl 8) and 0xFF00) + ((i shl 16) and 0xFF0000) + 0xFF000000).toInt()
    }

    private fun getByteValueFromLuminanceValue(luminance: Byte): Byte {
        return (((if (luminance < 0) 255 + luminance.toInt() else luminance.toInt()) - 16) * 1.1636)
            .toInt().toByte()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
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
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}