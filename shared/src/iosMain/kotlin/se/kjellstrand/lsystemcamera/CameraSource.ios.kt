@file:OptIn(ExperimentalForeignApi::class)

package se.kjellstrand.lsystemcamera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset352x288
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateDeviceGray
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetInterpolationQuality
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGInterpolationHigh
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddressOfPlane
import platform.CoreVideo.CVPixelBufferGetBytesPerRowOfPlane
import platform.CoreVideo.CVPixelBufferGetHeightOfPlane
import platform.CoreVideo.CVPixelBufferGetWidthOfPlane
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelBufferLock_ReadOnly
import platform.CoreVideo.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
actual fun rememberCameraSource(front: Boolean, onFrame: (LumaFrame) -> Unit) {
    val currentOnFrame by rememberUpdatedState(onFrame)
    val emit = remember { { frame: LumaFrame -> currentOnFrame(frame) } }
    // No capture device on the simulator; a picked photo stands in for the camera there.
    if (remember { AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) == null }) {
        PickedPhotoSource(front, emit)
    } else {
        CameraSession(front, emit)
    }
}

// ---------------------------------------------------------------- camera

@Composable
private fun CameraSession(front: Boolean, onFrame: (LumaFrame) -> Unit) {
    // AVFoundation holds the sample buffer delegate weakly; the composition is the strong ref.
    val delegate = remember(front) { VideoFrameDelegate(onFrame) }
    DisposableEffect(delegate) {
        val session = AVCaptureSession()
        val output = AVCaptureVideoDataOutput()
        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPreset352x288
        videoDevice(front)
            ?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, error = null) }
            ?.takeIf { session.canAddInput(it) }
            ?.let { session.addInput(it) }
        output.videoSettings = mapOf(PIXEL_FORMAT_KEY to kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange)
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(delegate, dispatch_queue_create("lsystem.camera", null))
        if (session.canAddOutput(output)) session.addOutput(output)
        session.commitConfiguration()
        // iOS 17 can rotate in the capture pipeline, so frames arrive portrait-upright and need
        // no rotation from us. Otherwise the sensor's landscape frame is rotated by the shared
        // readLuminance loop, whose 90 means the same clockwise turn CameraX reports on Android.
        output.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
            if (connection.isVideoRotationAngleSupported(UPRIGHT_ANGLE)) {
                connection.videoRotationAngle = UPRIGHT_ANGLE
                delegate.rotationDegrees = 0
            }
        }
        // startRunning blocks; AVFoundation wants it off the main thread.
        onBackgroundQueue { session.startRunning() }
        onDispose {
            output.setSampleBufferDelegate(null, null)
            onBackgroundQueue { session.stopRunning() }
        }
    }
}

private fun videoDevice(front: Boolean): AVCaptureDevice? {
    val position = if (front) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
    return AVCaptureDevice.defaultDeviceWithDeviceType(
        deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera,
        mediaType = AVMediaTypeVideo,
        position = position,
    ) ?: AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
}

private class VideoFrameDelegate(
    private val onFrame: (LumaFrame) -> Unit,
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    /** 0 once the capture connection rotates for us, 90 when it cannot. */
    var rotationDegrees = 90

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        val buffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: return
        CVPixelBufferLockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly)
        val base = CVPixelBufferGetBaseAddressOfPlane(buffer, 0u)?.reinterpret<ByteVar>()
        if (base != null) {
            onFrame(
                PixelBufferFrame(
                    base = base,
                    width = CVPixelBufferGetWidthOfPlane(buffer, 0u).toInt(),
                    height = CVPixelBufferGetHeightOfPlane(buffer, 0u).toInt(),
                    rowStride = CVPixelBufferGetBytesPerRowOfPlane(buffer, 0u).toInt(),
                    rotationDegrees = rotationDegrees,
                )
            )
        }
        CVPixelBufferUnlockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly)
    }
}

/** Plane 0 of a 420 bi-planar pixel buffer. Valid only while the buffer is locked. */
private class PixelBufferFrame(
    private val base: CPointer<ByteVar>,
    override val width: Int,
    override val height: Int,
    override val rowStride: Int,
    override val rotationDegrees: Int,
) : LumaFrame {
    override val pixelStride = 1
    override fun byteAt(offset: Int) = base[offset].toInt() and 0xFF
}

// ---------------------------------------------------------------- picked photo

/**
 * The simulator has no camera, so a photo from the library is the frame. The switch-camera button
 * doubles as "pick another photo": every flip of [front] re-presents the picker.
 */
@Composable
private fun PickedPhotoSource(front: Boolean, onFrame: (LumaFrame) -> Unit) {
    var frame by remember { mutableStateOf<LumaFrame?>(null) }
    LaunchedEffect(front) { presentPicker { frame = it?.toLumaFrame() } }
    val picked = frame
    if (picked != null) {
        // ponytail: re-emit the still on a ticker so the sliders redraw — the pipeline only
        // renders per frame. Push it into the ViewModel's frame flow instead if 10 fps of
        // re-rendering a static image ever shows up in a profile.
        LaunchedEffect(picked) {
            while (true) {
                withContext(Dispatchers.Default) { onFrame(picked) }
                delay(TICK_MS)
            }
        }
    }
}

private class ByteArrayFrame(
    private val bytes: ByteArray,
    override val width: Int,
    override val height: Int,
) : LumaFrame {
    override val rowStride = width
    override val pixelStride = 1
    override val rotationDegrees = 0
    override fun byteAt(offset: Int) = bytes[offset].toInt() and 0xFF
}

/**
 * Downscales to camera size and reads 8-bit grey straight out of a Core Graphics context, which
 * is the same strided single-channel plane the camera hands us. EXIF orientation is ignored: the
 * backing CGImage is drawn as it is stored, so a sideways photo renders sideways.
 */
private fun UIImage.toLumaFrame(): LumaFrame? {
    val cgImage = CGImage ?: return null
    val w = CGImageGetWidth(cgImage).toInt()
    val h = CGImageGetHeight(cgImage).toInt()
    if (w == 0 || h == 0) return null
    val scale = ANALYSIS_SIZE.toDouble() / max(w, h)
    val tw = max(1, (w * scale).roundToInt())
    val th = max(1, (h * scale).roundToInt())
    val space = CGColorSpaceCreateDeviceGray()
    val context = CGBitmapContextCreate(
        data = null,
        width = tw.convert(),
        height = th.convert(),
        bitsPerComponent = 8.convert(),
        bytesPerRow = tw.convert(),
        space = space,
        bitmapInfo = CGImageAlphaInfo.kCGImageAlphaNone.value,
    )
    CGColorSpaceRelease(space)
    if (context == null) return null
    CGContextSetInterpolationQuality(context, kCGInterpolationHigh)
    CGContextDrawImage(context, CGRectMake(0.0, 0.0, tw.toDouble(), th.toDouble()), cgImage)
    val bytes = CGBitmapContextGetData(context)?.readBytes(tw * th)
    CGContextRelease(context)
    return bytes?.let { ByteArrayFrame(it, tw, th) }
}

// PHPickerViewController holds its delegate weakly; park it until it fires.
private var pickerDelegate: PickerDelegate? = null

private fun presentPicker(onPicked: (UIImage?) -> Unit) {
    val configuration = PHPickerConfiguration().apply {
        filter = PHPickerFilter.imagesFilter
        selectionLimit = 1L
    }
    val picker = PHPickerViewController(configuration = configuration)
    val delegate = PickerDelegate(onPicked)
    pickerDelegate = delegate
    picker.delegate = delegate
    keyWindow()?.rootViewController?.presentViewController(picker, animated = true, completion = null)
}

private class PickerDelegate(
    private val onPicked: (UIImage?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider ?: return
        // The class-typed loadObjectOfClass(UIImage) does not map through cinterop; the raw
        // bytes do, and UIImage(data:) reads every format the picker hands out.
        if (!provider.hasItemConformingToTypeIdentifier("public.image")) return
        provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            // The completion runs on a background queue; Compose state wants the main thread.
            dispatch_async(dispatch_get_main_queue()) { onPicked(data?.let { UIImage(data = it) }) }
        }
    }
}

private fun onBackgroundQueue(block: () -> Unit) =
    dispatch_async(dispatch_get_global_queue(0.convert(), 0.convert()), block)

/** The value of `kCVPixelBufferPixelFormatTypeKey`, which is a CFStringRef and so does not
 * bridge as a Kotlin map key. */
private const val PIXEL_FORMAT_KEY = "PixelFormatType"
private const val UPRIGHT_ANGLE = 90.0
private const val ANALYSIS_SIZE = 200
private const val TICK_MS = 100L
