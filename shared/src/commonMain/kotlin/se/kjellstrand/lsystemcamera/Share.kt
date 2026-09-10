package se.kjellstrand.lsystemcamera

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Hands the image to the platform share sheet as a PNG. */
@Composable
expect fun rememberShareImage(): (ImageBitmap) -> Unit
