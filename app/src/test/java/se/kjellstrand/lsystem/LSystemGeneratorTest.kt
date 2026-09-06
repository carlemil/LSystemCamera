package se.kjellstrand.lsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.kjellstrand.lsystem.model.LSystem

class LSystemGeneratorTest {

    @Test
    fun everySystemGeneratesNormalizedPolylineAtMinAndMaxIterations() {
        for (system in LSystem.systems) {
            for (iterations in listOf(system.minIterations, system.maxIterations)) {
                val line = LSystemGenerator.generatePolygon(system, iterations)
                assertTrue("${system.name}@$iterations too short", line.size > 2)
                val outside = line.filter { it.x !in -EPS..1 + EPS || it.y !in -EPS..1 + EPS }
                assertTrue("${system.name}@$iterations has points outside 0..1: $outside", outside.isEmpty())
            }
        }
    }

    @Test
    fun hullHasTwoPointsPerInnerSegment() {
        val line = LSystemGenerator.generatePolygon(LSystem.getByName("Hilbert"), 3)
        val hull = buildHullFromPolygon(line)
        assertEquals(2 * (line.size - 2), hull.size)
    }

    private companion object {
        const val EPS = 1e-9
    }
}
