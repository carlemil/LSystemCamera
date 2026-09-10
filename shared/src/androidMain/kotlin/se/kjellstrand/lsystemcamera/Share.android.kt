package se.kjellstrand.lsystemcamera

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberShareImage(): (ImageBitmap) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { image ->
            val activity = context.findActivity()
            val file = File(activity.cacheDir, "imageview").apply { mkdirs() }.resolve("image.png")
            // Copy first: the renderer keeps drawing into its two buffers while we compress.
            file.outputStream().use {
                Bitmap.createBitmap(image.asAndroidBitmap()).compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            ShareCompat.IntentBuilder(activity)
                .setType("image/png")
                .setStream(FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file))
                .startChooser()
        }
    }
}

/** `startChooser()` needs an Activity, and LocalContext may be a wrapper around one. */
private fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    error("No Activity in the context chain")
}
