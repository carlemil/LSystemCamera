package se.kjellstrand.lsystemcamera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberCameraPermission(): CameraPermissionState {
    // No capture device (the simulator) means nothing to authorise: the picker source stands in
    // for the camera, and the UI must not sit on the rationale screen waiting for a grant.
    val hasDevice = remember { AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null }
    var status by remember {
        mutableStateOf(
            if (hasDevice) {
                AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
            } else {
                AVAuthorizationStatusAuthorized
            }
        )
    }
    return CameraPermissionState(
        granted = status == AVAuthorizationStatusAuthorized,
        request = {
            // iOS asks once; after a denial this is a no-op and only Settings can flip it.
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { allowed ->
                dispatch_async(dispatch_get_main_queue()) {
                    status = if (allowed) AVAuthorizationStatusAuthorized else AVAuthorizationStatusDenied
                }
            }
        },
        openSettings = {
            UIApplication.sharedApplication.openURL(
                NSURL(string = UIApplicationOpenSettingsURLString),
                options = mapOf<Any?, Any>(),
                completionHandler = null,
            )
        },
    )
}
