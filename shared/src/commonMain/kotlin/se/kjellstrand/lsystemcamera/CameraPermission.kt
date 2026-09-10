package se.kjellstrand.lsystemcamera

import androidx.compose.runtime.Composable

class CameraPermissionState(
    val granted: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

/** [CameraPermissionState.granted] is re-read whenever the app resumes, so returning from the
 * system dialog or from Settings updates the UI. */
@Composable
expect fun rememberCameraPermission(): CameraPermissionState
