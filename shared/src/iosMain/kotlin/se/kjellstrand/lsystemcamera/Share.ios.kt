@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package se.kjellstrand.lsystemcamera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberShareImage(): (ImageBitmap) -> Unit = remember {
    { image ->
        // makeFromBitmap copies the pixels; the renderer keeps drawing into its two buffers.
        val png = Image.makeFromBitmap(image.asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)
            ?.bytes
        val root = keyWindow()?.rootViewController
        if (png != null && png.isNotEmpty() && root != null) {
            val data = png.usePinned {
                NSData.create(bytes = it.addressOf(0), length = png.size.convert())
            }
            val sheet = UIActivityViewController(
                activityItems = listOf(UIImage(data = data)),
                applicationActivities = null,
            )
            // On iPad the sheet is a popover and traps without an anchor.
            sheet.popoverPresentationController?.sourceView = root.view
            root.presentViewController(sheet, animated = true, completion = null)
        }
    }
}

/** The app's key window; a sheet (or the photo picker) has nowhere to go without one. */
internal fun keyWindow(): UIWindow? =
    UIApplication.sharedApplication.connectedScenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { it.windows }
        .filterIsInstance<UIWindow>()
        .firstOrNull()
