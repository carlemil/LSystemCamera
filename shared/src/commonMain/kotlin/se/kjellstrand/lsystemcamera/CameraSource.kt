package se.kjellstrand.lsystemcamera

import androidx.compose.runtime.Composable

/**
 * Binds the camera for as long as this composition lives, delivering frames to [onFrame] on the
 * platform's own delivery thread (which also provides the backpressure: one frame at a time,
 * keep-latest). Rebinds when [front] flips.
 */
@Composable
expect fun rememberCameraSource(front: Boolean, onFrame: (LumaFrame) -> Unit)
