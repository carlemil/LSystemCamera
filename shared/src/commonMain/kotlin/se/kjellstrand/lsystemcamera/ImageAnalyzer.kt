package se.kjellstrand.lsystemcamera

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import se.kjellstrand.lsystem.LSystemGenerator
import se.kjellstrand.lsystem.buildHullFromPolygon
import se.kjellstrand.lsystem.getMidPoint
import se.kjellstrand.lsystem.model.LSTriple
import se.kjellstrand.lsystem.model.LSystem
import se.kjellstrand.lsystemcamera.viewmodel.UiState
import kotlin.math.min
import kotlin.math.pow

/**
 * Turns one camera frame into one rendered bitmap. Single-threaded: only ever called from
 * the camera's delivery thread. Double-buffered so the UI never reads the bitmap being drawn.
 */
class ImageAnalyzer {

    private val buffers = arrayOf(ImageBitmap(SIZE, SIZE), ImageBitmap(SIZE, SIZE))
    private var current = 0
    private var luminance: Array<DoubleArray> = emptyArray()

    private var line: MutableList<LSTriple> = mutableListOf()
    private var cachedSystem: LSystem? = null
    private var cachedIterations = -1
    private var minWidth = 0.0
    private var maxWidth = 0.0

    private val bgPaint = Paint().apply { color = Color.White }
    private val linePaint = Paint().apply {
        color = Color.Black
        style = PaintingStyle.Fill
        isAntiAlias = true
    }
    private val toPixels = Matrix().apply { scale(SIZE.toFloat(), SIZE.toFloat()) }

    fun analyze(image: LumaFrame, state: UiState): ImageBitmap {
        if (state.system !== cachedSystem || state.iterations != cachedIterations) {
            cachedSystem = state.system
            cachedIterations = state.iterations
            val (min, max) = LSystemGenerator.getRecommendedMinAndMaxWidth(state.iterations, state.system)
            minWidth = min
            maxWidth = max
            line = LSystemGenerator.generatePolygon(state.system, state.iterations).distinct().toMutableList()
            LSystemGenerator.addSideBuffer(maxWidth + 0.02, line)
        }
        readLuminance(image)

        // brightness: 1 == neutral. contrast: 1 == full width range, 0 == uniform width.
        val brightness = 2.0.pow(state.brightness.toDouble())
        val contrast = (1 - state.contrast) * (maxWidth + minWidth) / 2
        LSystemGenerator.setLineWidthAccordingToImage(
            line = line,
            luminanceData = luminance,
            minWidth = (minWidth + contrast) * brightness,
            maxWidth = (maxWidth - contrast) * brightness
        )
        LSystemGenerator.smoothenWidthOfLine(line)

        current = 1 - current
        val bitmap = buffers[current]
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), bgPaint)
        canvas.drawPath(quadPath(buildHullFromPolygon(line)), linePaint)
        return bitmap
    }

    /**
     * Center-cropped square of the Y plane, rotated to the display orientation, inverted so
     * dark == 1. `luminance[sx][sy]` is indexed in screen space. The portrait lock is ignored on
     * large screens from Android 17 (targetSdk 37), so the rotation is read per frame.
     */
    private fun readLuminance(image: LumaFrame) {
        val side = min(image.width, image.height)
        if (luminance.size != side) luminance = Array(side) { DoubleArray(side) }
        val rowStride = image.rowStride
        val pixelStride = image.pixelStride
        val xOffset = (image.width - side) / 2
        val yOffset = (image.height - side) / 2
        val rotation = image.rotationDegrees
        val last = side - 1
        for (cy in 0 until side) {
            val row = (cy + yOffset) * rowStride + xOffset * pixelStride
            for (cx in 0 until side) {
                val value = 1 - image.byteAt(row + cx * pixelStride) / 256.0
                when (rotation) {
                    90 -> luminance[last - cy][cx] = value
                    180 -> luminance[last - cx][last - cy] = value
                    270 -> luminance[cy][last - cx] = value
                    else -> luminance[cx][cy] = value
                }
            }
        }
    }

    private fun quadPath(hull: List<LSTriple>): Path {
        val path = Path()
        val start = getMidPoint(hull[hull.size - 1], hull[hull.size - 2])
        path.moveTo(start.x.toFloat(), start.y.toFloat())
        for (i in hull.indices) {
            val control = hull[(if (i == 0) hull.size else i) - 1]
            val end = getMidPoint(control, hull[i])
            path.quadraticTo(control.x.toFloat(), control.y.toFloat(), end.x.toFloat(), end.y.toFloat())
        }
        path.transform(toPixels)
        path.close()
        return path
    }

    private companion object {
        const val SIZE = 1024
    }
}
