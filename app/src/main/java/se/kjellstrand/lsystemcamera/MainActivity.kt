package se.kjellstrand.lsystemcamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import androidx.core.content.FileProvider
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraXBasic"

        private const val CAMERA_IMAGE_SIZE = 200
        private const val REQUEST_CODE_PERMISSIONS = 10

        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    private lateinit var cameraExecutor: ExecutorService
    private val model: LSystemViewModel by viewModels()
    private val analyzerExecutor = Executors.newSingleThreadExecutor()

    private var bitmap: Bitmap? = null

    @androidx.camera.core.ExperimentalGetImage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestCameraPermissions()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.overflow_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.share -> {
                bitmap?.let { shareImage(it) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareImage(bitmap: Bitmap) {
        val imageView = "imageview"
        val imageName = "image.png"
        // save bitmap to cache directory
        try {
            val cachePath = File(cacheDir, imageView)
            // don't forget to make the directory
            cachePath.mkdirs()
            // overwrites this image every time
            val stream = FileOutputStream("$cachePath/$imageName")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val imagePath = File(cacheDir, imageView)
        val newFile = File(imagePath, imageName)
        val contentUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", newFile)
        if (contentUri != null) {
            val shareIntent = Intent()
            val chooser = Intent.createChooser(shareIntent, getString(R.string.share))
            val resInfoList = this.packageManager.queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                grantUriPermission(packageName, contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            shareIntent.action = Intent.ACTION_SEND
            // temp permission for receiving app to read this file
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(contentUri, contentResolver?.getType(contentUri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Choose an app"))
        }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
