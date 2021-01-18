package se.kjellstrand.lsystemcamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
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
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import se.kjellstrand.lsystem.LSystemGenerator
import se.kjellstrand.lsystem.LSystemGenerator.generatePolygon
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.variablewidthline.buildHullFromPolygon
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val CAMERA_IMAGE_SIZE = 200

class MainActivity : AppCompatActivity() {
    private val iteration = 7

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var lSystem: LSystem
    private lateinit var line: List<Triple<Float, Float, Float>>

    private val model: LSystemViewModel by viewModels()

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
        cameraExecutor = Executors.newSingleThreadExecutor()
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
            analyzeImage(image, size)
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

    private fun analyzeImage(image: ImageProxy, size: Int) {
        if (luminance.size != image.width || luminance[0].size != image.height) {
            luminance = Array(image.height) { FloatArray(image.width) }
        }

        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
        }


        bitmap?.let { bitmap ->

            val scaledVWLine = updateLSystem(image, bitmap.width, bitmap.height, size)

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

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun updateLSystem(
        image: ImageProxy,
        width: Int,
        height: Int,
        size: Int
    ): List<Triple<Float, Float, Float>> {
        val plane = image.image?.planes?.get(0)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) { // TODO go from 10..90 using rowStride and imageWidth to figure out start and stop positions
                val byte = (plane?.buffer?.get(x + y * plane.rowStride) ?: Byte.MIN_VALUE)
                val f = byte.toFloat() / 256f
                luminance[image.height - y - 1][x] = 1 - if (f < 0) f + 1 else f
            }
        }

        val (minWidth, maxWidth) = LSystemGenerator.getRecommendedMinAndMaxWidth(
            width, iteration, lSystem
        )

        val vWLine = LSystemGenerator.setLineWidthAccordingToImage(
            line = line,
            luminanceData = luminance,
            minWidth = minWidth,
            maxWidth = maxWidth
        )

        val outputSideBuffer = width / 50
        val adjustedLine =
            LSystemGenerator.adjustToOutputRectangle(width, outputSideBuffer, vWLine)

        return adjustedLine.map { p ->
            // TODO stop making new Triple
            Triple(p.first * width, p.second * height, p.third)
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