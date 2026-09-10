package se.kjellstrand.lsystem

import org.junit.Test
import se.kjellstrand.lsystem.model.LSTriple
import se.kjellstrand.lsystem.model.LSystem
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.hypot

/**
 * Not a test: prints, per curve and iteration, the spacing between neighbouring strands and how
 * much of it the current max width fills, so lineWidthExp / lineWidthBold / defaultIterations
 * can be set by numbers instead of by eye. Runs with jvmTest.
 */
class CurveTuningTest {

    @Test
    fun printStrandSpacing() {
        for (system in LSystem.systems) {
            println("== ${system.name} exp=${system.lineWidthExp} bold=${system.lineWidthBold} default=${system.defaultIterations}")
            var prev = 0.0
            for (i in system.minIterations..system.maxIterations) {
                val line = LSystemGenerator.generatePolygon(unsmoothed(system), i).distinct()
                val gap = strandGap(line)
                val max = LSystemGenerator.getRecommendedMinAndMaxWidth(i, system).second
                val fill = 2 * max / (gap * (1 - 2 * (max + 0.02)))
                println("  it=%2d n=%6d gap=%.5f ratio=%.3f max=%.5f fill=%.2f".format(i, line.size, gap, if (prev > 0) prev / gap else 0.0, max, fill))
                prev = gap
            }
        }
    }

    /**
     * Worst case per curve: every iteration drawn at max width through the real hull pipeline, tiled
     * into build/curve-tuning/<Name>.png with the default iteration framed. Strands must not merge.
     */
    @Test
    fun renderWorstCase() {
        val dir = File("build/curve-tuning").apply { mkdirs() }
        for (system in LSystem.systems) {
            val iterations = (system.maxIterations - 3).coerceAtLeast(system.minIterations)..system.maxIterations
            val img = BufferedImage(TILE * iterations.count(), TILE, BufferedImage.TYPE_INT_RGB)
            val g = img.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color.WHITE
            g.fillRect(0, 0, img.width, img.height)
            iterations.forEachIndexed { tile, i ->
                val max = LSystemGenerator.getRecommendedMinAndMaxWidth(i, system).second
                val line = LSystemGenerator.generatePolygon(system, i).distinct().toMutableList()
                LSystemGenerator.addSideBuffer(max + 0.02, line)
                line.forEach { it.w = max }
                val hull = buildHullFromPolygon(line)
                val path = Path2D.Double()
                val start = getMidPoint(hull[hull.size - 1], hull[hull.size - 2])
                path.moveTo(start.x * TILE + tile * TILE, start.y * TILE)
                for (j in hull.indices) {
                    val c = hull[(if (j == 0) hull.size else j) - 1]
                    val e = getMidPoint(c, hull[j])
                    path.quadTo(c.x * TILE + tile * TILE, c.y * TILE, e.x * TILE + tile * TILE, e.y * TILE)
                }
                g.color = Color.BLACK
                g.fill(path)
                g.color = if (i == system.defaultIterations) Color.RED else Color.LIGHT_GRAY
                g.drawRect(tile * TILE, 0, TILE - 1, TILE - 1)
                g.drawString("$i", tile * TILE + 4, 14)
            }
            g.dispose()
            ImageIO.write(img, "png", File(dir, "${system.name}.png"))
        }
    }

    private fun unsmoothed(s: LSystem) = LSystem(s.name, s.angle, s.rules, s.forwardChars, s.axiom, s.lineWidthExp, s.lineWidthBold, s.minIterations, s.maxIterations, s.defaultIterations, 0)

    /** 10th percentile of each vertex's distance to the nearest vertex not adjacent along the path. */
    private fun strandGap(line: List<LSTriple>): Double {
        val step = line.zipWithNext { a, b -> hypot(a.x - b.x, a.y - b.y) }.average()
        val cells = HashMap<Long, MutableList<Int>>()
        fun key(x: Double, y: Double) = ((x / step).toLong() shl 32) or ((y / step).toLong() and 0xffffffffL)
        line.forEachIndexed { i, p -> cells.getOrPut(key(p.x, p.y)) { mutableListOf() }.add(i) }
        val nearest = DoubleArray(line.size) { Double.MAX_VALUE }
        line.forEachIndexed { i, p ->
            val cx = (p.x / step).toLong()
            val cy = (p.y / step).toLong()
            for (dx in -1L..1L) for (dy in -1L..1L) {
                cells[((cx + dx) shl 32) or ((cy + dy) and 0xffffffffL)]?.forEach { j ->
                    if (kotlin.math.abs(i - j) > 2) {
                        val d = hypot(p.x - line[j].x, p.y - line[j].y)
                        if (d > 1e-9 && d < nearest[i]) nearest[i] = d
                    }
                }
            }
        }
        val sorted = nearest.filter { it < Double.MAX_VALUE }.sorted()
        return if (sorted.isEmpty()) Double.NaN else sorted[sorted.size / 10]
    }

    private companion object {
        const val TILE = 400
    }
}
