package se.kjellstrand.lsystemcamera

import android.annotation.SuppressLint
import android.graphics.*
import android.widget.ImageView
import androidx.camera.core.ImageProxy
import se.kjellstrand.lsystem.LSystemGenerator
import se.kjellstrand.lsystem.buildHullFromPolygon
import se.kjellstrand.lsystem.getMidPoint
import se.kjellstrand.lsystem.model.LSTriple
import se.kjellstrand.lsystemcamera.viewmodel.LSystemViewModel
import kotlin.math.pow

class ImageAnalyzer {

    companion object {
        private var bitmap: Bitmap? = null
        private var bitmapDoubleBuffer1: Bitmap? = null
        private var bitmapDoubleBuffer2: Bitmap? = null
        private var luminance: Array<DoubleArray> = Array(0) { DoubleArray(0) }
        private lateinit var line: MutableList<LSTriple>

        fun updateLSystem(
            model: LSystemViewModel
        ) {
            model.getLSystem()?.let { system ->
                line = LSystemGenerator.generatePolygon(system, model.getIterations())
                line.distinct()
            }
        }

        fun analyzeImage(
            image: ImageProxy,
            imageView: ImageView,
            model: LSystemViewModel
        ): Bitmap? {
            if (luminance.size != image.width || luminance[0].size != image.height) {
                luminance = Array(image.height) { DoubleArray(image.height) }
            }

            if (bitmapDoubleBuffer1 == null || bitmapDoubleBuffer2 == null) {
                bitmapDoubleBuffer1 = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
                bitmapDoubleBuffer2 = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
            }
            bitmap = if (bitmap == bitmapDoubleBuffer1) {
                bitmapDoubleBuffer2
            } else {
                bitmapDoubleBuffer1
            }

            bitmap?.let { bitmap ->
                model.getLSystem()?.let { system ->
                    line = LSystemGenerator.generatePolygon(system, model.getIterations())
                    line = line.distinct() as MutableList<LSTriple>

                    val scaledLine = updateLSystem(image, luminance, model, line)

                    val hull = buildHullFromPolygon(scaledLine)

                    val c = Canvas(bitmap)

                    // Set style and color for the background
                    val bgPaint = Paint()
                    bgPaint.color = Color.WHITE
                    c.drawRect(0F, 0F, c.width.toFloat(), c.height.toFloat(), bgPaint)

                    // Set style and color for the line
                    val paint = Paint()
                    paint.color = Color.BLACK
                    paint.style = Paint.Style.FILL_AND_STROKE

                    // Measure performance impact and decide if we should keep isAntiAlias
                    paint.isAntiAlias = true

                    c.drawPath(createQuadCurveFromHull(hull, imageView), paint)
                }
            }
            image.close()
            return bitmap
        }

        private fun createQuadCurveFromHull(
            hull: MutableList<LSTriple>,
            imageView: ImageView
        ): Path {
            val path = Path()
            val polygonInitialPP = getMidPoint(hull[hull.size - 1], hull[hull.size - 2])
            path.moveTo(polygonInitialPP.x.toFloat(), polygonInitialPP.y.toFloat())

            for (i in 0 until hull.size) {
                val quadStartPP = hull[(if (i == 0) hull.size else i) - 1]
                val nextQuadStartPP = hull[i]
                val quadEndPP = getMidPoint(quadStartPP, nextQuadStartPP)
                path.quadTo(quadStartPP.x.toFloat(), quadStartPP.y.toFloat(), quadEndPP.x.toFloat(), quadEndPP.y.toFloat())
            }
            val matrix = Matrix()
            matrix.postScale(imageView.width.toFloat(), imageView.height.toFloat())
            path.transform(matrix)
            path.close()
            return path
        }

        @SuppressLint("UnsafeExperimentalUsageError")
        fun updateLSystem(
            image: ImageProxy,
            luminance: Array<DoubleArray>,
            model: LSystemViewModel,
            line: MutableList<LSTriple>
        ): MutableList<LSTriple> {
            val plane = image.image?.planes?.get(0)

            // TODO Check if w > h and do opposite
            val vhDiff = (image.width - image.height) / 2
            val hRange = 0 until image.height
            val vRange = vhDiff until image.width - vhDiff
            for (y in hRange) {
                for (x in vRange) {
                    val byte = (plane?.buffer?.get(x + y * plane.rowStride) ?: Byte.MIN_VALUE)
                    val f = byte.toDouble() / 256.0
                    luminance[image.height - y - 1][x - vhDiff] = 1 - if (f < 0) f + 1 else f
                }
            }
            // TODO END

            val maxWidth = model.getMaxWidth()
            val minWidth = model.getMinWidth()
            // 1 == full brightness, 0 == lowest brightness
            val brightness = 2.0.pow(model.getBrightnessMod())

            // 0 == full contrast, max width diff, 1 == no contrast, width is same all over
            val contrast = (1 - model.getContrastMod()) * (maxWidth + minWidth) / 2f

            LSystemGenerator.setLineWidthAccordingToImage(
                line = line,
                luminanceData = luminance,
                minWidth = (minWidth + contrast) * brightness,
                maxWidth = (maxWidth - contrast) * brightness
            )

            LSystemGenerator.smoothenWidthOfLine(line)

            val outputSideBuffer = maxWidth + 0.02f

            LSystemGenerator.addSideBuffer(outputSideBuffer, line)

            return line
        }
    }
}
