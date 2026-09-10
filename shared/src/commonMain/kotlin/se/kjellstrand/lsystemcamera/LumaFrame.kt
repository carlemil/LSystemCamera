package se.kjellstrand.lsystemcamera

/**
 * One camera frame's luminance plane, in the layout both platforms already hand us: Android's
 * `ImageProxy.planes[0]` and iOS' `CVPixelBuffer` plane 0 are both a strided Y plane.
 */
interface LumaFrame {
    val width: Int
    val height: Int
    val rowStride: Int
    val pixelStride: Int
    /** Clockwise rotation needed to bring the frame to the display orientation. */
    val rotationDegrees: Int
    /** The unsigned byte at [offset] into the plane, 0..255. */
    fun byteAt(offset: Int): Int
}
