package se.kjellstrand.lsystem

import org.junit.Test
import se.kjellstrand.lsystem.model.LSTriple
import se.kjellstrand.lsystem.model.LSystem
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.hypot
import javax.imageio.ImageIO

/**
 * Not really a test: renders every catalog curve to app/build/curve-previews/ so new rules can be
 * eyeballed and the `-icon` files copied into res/drawable-nodpi. Runs with testDebugUnitTest.
 */
class CurvePreviewTest {

    @Test
    fun renderPreviews() {
        val dir = File("build/curve-previews").apply { mkdirs() }
        for (system in LSystem.systems) {
            render(system, iconIterations(system), File(dir, "${system.name}-icon.png"))
            render(system, system.maxIterations, File(dir, "${system.name}-max.png"))
        }
    }

    /** Largest iteration that still reads as a shape at 256px: the last one under ~300 vertices. */
    private fun iconIterations(system: LSystem): Int {
        iconOverrides[system.name]?.let { return it }
        var best = system.minIterations
        for (i in system.minIterations..system.maxIterations) {
            if (LSystemGenerator.generatePolygon(system, i).size > 300) break
            best = i
        }
        return best
    }

    private fun render(system: LSystem, iterations: Int, out: File) {
        // No distinct() here: retracing curves (Cross, Tiles) would get straight jumps between the
        // surviving points. Stroke follows segment length so every icon has similar line weight.
        val line = LSystemGenerator.generatePolygon(system, iterations)
        val segment = line.zipWithNext { a, b -> hypot(a.x - b.x, a.y - b.y) }.average()
        val width = (segment * SIZE * 0.3).coerceIn(2.5, 10.0)
        LSystemGenerator.addSideBuffer(width / SIZE + 0.04, line)
        val img = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB)
        img.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            color = Color.WHITE
            fillRect(0, 0, SIZE, SIZE)
            color = Color.BLACK
            stroke = BasicStroke(width.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            draw(path(line))
            dispose()
        }
        ImageIO.write(img, "png", out)
    }

    private fun path(line: List<LSTriple>) = Path2D.Double().apply {
        moveTo(line[0].x * SIZE, line[0].y * SIZE)
        for (p in line.drop(1)) lineTo(p.x * SIZE, p.y * SIZE)
    }

    private companion object {
        const val SIZE = 256
        /** Curves whose auto-picked iteration reads badly as an icon. */
        val iconOverrides = mapOf("QuadraticGosper" to 2)
    }
}
