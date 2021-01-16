package se.kjellstrand.lsystemcamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
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
import se.kjellstrand.lsystem.LSystemGenerator.generatePolygon
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.variablewidthline.buildHullFromPolygon
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val CAMERA_IMAGE_SIZE = 200

class MainActivity : AppCompatActivity() {
    private val iteration = 7

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var lSystem: LSystem
    private lateinit var line: List<Triple<Float, Float, Float>>

    private val analyzerExecutor = Executors.newSingleThreadExecutor()

    private var luminance: Array<FloatArray> = Array(0) { FloatArray(0) }
    private var bitmap: Bitmap? = null

    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lSystem = LSystem.getByName("Hilbert")!!

        line = generatePolygon(lSystem, iteration).map { p ->
            Triple(p.first, p.second, 1F)
        }.distinct()

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
            //it.setSurfaceProvider(viewFinder.createSurfaceProvider())
            it?.setSurfaceProvider(viewFinder.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            //.setTargetAspectRatio(screenAspectRatio)
            .build()

        // Select back camera as a default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(size, size))
            .setTargetRotation(Surface.ROTATION_0) // TODO Figure out why this is broken and do not work.
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
        // val rotationDegrees = image.imageInfo.rotationDegrees

        if (luminance.size != image.width || luminance[0].size != image.height) {
            luminance = Array(image.height) { FloatArray(image.width) }
        }

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
        }

        bitmap?.let { bitmap ->

            var mi = 0f
            var ma = 0f

            val plane = image.image?.planes?.get(0)
            for (y in 0 until image.height) {
                for (x in 0 until image.width) { // TODO go from 10..90 using rowStride and imageWidth to figure out start and stop positions
                    val byte = (plane?.buffer?.get(x + y * plane.rowStride) ?: Byte.MIN_VALUE)
                    val f = byte.toFloat() / 256f
                    luminance[image.height - y - 1][x] = 1 - if (f < 0) f + 1 else f
                }
            }

            println("mi : " + mi + " ma " + ma)

            val (minWidth, maxWidth) = LSystemGenerator.getRecommendedMinAndMaxWidth(
                bitmap.width, iteration, lSystem
            )

            if (minWidth < 0.5 || minWidth < size / 5000) {
                //return
            }
            val vWLine = LSystemGenerator.setLineWidthAccordingToImage(
                line = line,
                luminanceData = luminance,
                minWidth = minWidth,
                maxWidth = maxWidth
            )

            val outputSideBuffer = bitmap.width / 50
            val adjustedLine =
                LSystemGenerator.adjustToOutputRectangle(bitmap.width, outputSideBuffer, vWLine)

            val scaledVWLine = adjustedLine.map { p ->
                // TODO stop making new Triple
                Triple(p.first * bitmap.width, p.second * bitmap.height, p.third)
            }

            val hull = buildHullFromPolygon(scaledVWLine)

            val c = Canvas(bitmap)
            val bgPaint = Paint()
            bgPaint.color = Color.WHITE
            c.drawRect(0F, 0F, c.width.toFloat(), c.height.toFloat(), bgPaint)
            val paint = Paint()
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL_AND_STROKE

            val polyPath = Path()
            //polyPath.fillType = Path.FillType.WINDING
            polyPath.moveTo(hull[0].first, hull[0].second)
            hull.forEach { p ->
                polyPath.lineTo(p.first, p.second)
            }
            polyPath.close()
            c.drawPath(polyPath, paint)
        }

        runOnUiThread {
            imageView.setImageBitmap(bitmap)
        }

        image.close()
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