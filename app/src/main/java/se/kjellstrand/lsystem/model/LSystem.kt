package se.kjellstrand.lsystem.model

import kotlin.math.PI

class LSystem(
    val name: String,
    val angle: Double,
    val rules: Map<String, String>,
    val forwardChars: Set<String>,
    val axiom: String,
    val lineWidthExp: Double,
    val lineWidthBold: Double,
    var minIterations: Int,
    var maxIterations: Int,
    var intermediateSplines: Int
) {
    fun getAngleInRadians(): Double {
        return angle / 180 * PI
    }

    companion object {

        fun getByName(name: String): LSystem {
            return this.systems.find { lsd -> lsd.name.startsWith(name, true) } ?: systems[0]
        }

        val systems: List<LSystem> = listOf(
            LSystem(
                name = "KochSnowFlake",
                angle = 90.0,
                rules = mapOf("F" to "F-F+F+FF-F-F+F"),
                forwardChars = setOf("F"),
                axiom = "F",
                lineWidthExp = 1.5,
                lineWidthBold = 0.2,
                minIterations = 1,
                maxIterations = 5,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Dragon",
                angle = 90.0,
                rules = mapOf("A" to "A+BF", "B" to "FA-B"),
                forwardChars = setOf("F"),
                axiom = "FA",
                lineWidthExp = 1.45,
                lineWidthBold = 0.1,
                minIterations = 1,
                maxIterations = 13,
                intermediateSplines = 3
            ),
            LSystem(
                name = "TwinDragon",
                angle = 90.0,
                rules = mapOf("A" to "A+BF", "B" to "FA-B"),
                forwardChars = setOf("F"),
                axiom = "FA+FA+",
                lineWidthExp = 1.45,
                lineWidthBold = 0.1,
                minIterations = 1,
                maxIterations = 14,
                intermediateSplines = 3
            ),
            LSystem(
                name = "Fudgeflake",
                angle = 30.0,
                rules = mapOf("F" to "+F----F++++F-"),
                forwardChars = setOf("F"),
                axiom = "F++++F++++F",
                lineWidthExp = 1.667,
                lineWidthBold = 0.15,
                minIterations = 1,
                maxIterations = 8,
                intermediateSplines = 1
            ),
            LSystem(
                name = "Hilbert",
                angle = 90.0,
                rules = mapOf("A" to "-BF+AFA+FB-", "B" to "+AF-BFB-FA+"),
                forwardChars = setOf("F"),
                axiom = "A",
                lineWidthExp = 2.0,
                lineWidthBold = 0.5,
                minIterations = 1,
                maxIterations = 8,
                intermediateSplines = 0
            ),
            LSystem(
                name = "SierpinskiTriangle",
                angle = 60.0,
                rules = mapOf("A" to "BF-AF-B", "B" to "AF+BF+A"),
                forwardChars = setOf("F"),
                axiom = "A",
                lineWidthExp = 2.0,
                lineWidthBold = 0.4,
                minIterations = 1,
                maxIterations = 9,
                intermediateSplines = 0
            ),
            LSystem(
                name = "SierpinskiCurve",
                angle = 45.0,
                rules = mapOf("X" to "XF+G+XF--F--XF+G+X"),
                forwardChars = setOf("F", "G"),
                axiom = "F--XF--F--XF",
                lineWidthExp = 2.0,
                lineWidthBold = 0.15,
                minIterations = 1,
                maxIterations = 7,
                intermediateSplines = 0
            ),
            LSystem(
                name = "SierpinskiSquare",
                angle = 90.0,
                rules = mapOf("X" to "XF-F+F-XF+F+XF-F+F-X"),
                forwardChars = setOf("F"),
                axiom = "F+XF+F+XF",
                lineWidthExp = 2.0,
                lineWidthBold = 0.15,
                minIterations = 1,
                maxIterations = 7,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Gosper",
                angle = 60.0,
                rules = mapOf("A" to "A-B--B+A++AA+B-", "B" to "+A-BB--B-A++A+B"),
                forwardChars = setOf("A", "B"),
                axiom = "A",
                lineWidthExp = 2.5,
                lineWidthBold = 0.28,
                minIterations = 1,
                maxIterations = 5,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Peano",
                angle = 90.0,
                rules = mapOf("L" to "LFRFL-F-RFLFR+F+LFRFL", "R" to "RFLFR+F+LFRFL-F-RFLFR"),
                forwardChars = setOf("F"),
                axiom = "L",
                lineWidthExp = 3.0,
                lineWidthBold = 0.35,
                minIterations = 1,
                maxIterations = 5,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Moore",
                angle = 90.0,
                rules = mapOf("A" to "-BF+AFA+FB-", "B" to "+AF-BFB-FA+"),
                forwardChars = setOf("F"),
                axiom = "AFA+F+AFA",
                lineWidthExp = 2.0,
                lineWidthBold = 0.2,
                minIterations = 1,
                maxIterations = 7,
                intermediateSplines = 0
            ),
            LSystem(
                name = "QuadraticGosper",
                angle = 90.0,
                rules = mapOf("X" to "XFX-YF-YF+FX+FX-YF-YFFX+YF+FXFXYF-FX+YF+FXFX+YF-FXYF-YF-FX+FX+YFYF-",
                    "Y" to "+FXFX-YF-YF+FX+FXYF+FX-YFYF-FX-YF+FXYFYF-FX-YFFX+FX+YF-YF-FX+FX+YFY"),
                forwardChars = setOf("F"),
                axiom = "-YF",
                lineWidthExp = 4.0,
                lineWidthBold = 0.2,
                minIterations = 1,
                maxIterations = 3,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Cross",
                angle = 90.0,
                rules = mapOf("F" to "F+FF++F+F"),
                forwardChars = setOf("F"),
                axiom = "F+F+F+F",
                lineWidthExp = 3.0,
                lineWidthBold = 0.2,
                minIterations = 1,
                maxIterations = 6,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Pentaplexity",
                angle = 36.0,
                rules = mapOf("F" to "F++F++F+++++F-F++F"),
                forwardChars = setOf("F"),
                axiom = "F++F++F++F++F",
                lineWidthExp = 3.0,
                lineWidthBold = 0.2,
                minIterations = 1,
                maxIterations = 5,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Tiles",
                angle = 90.0,
                rules = mapOf("F" to "FF+F-F+F+FF"),
                forwardChars = setOf("F"),
                axiom = "F+F+F+F",
                lineWidthExp = 3.0,
                lineWidthBold = 0.2,
                minIterations = 1,
                maxIterations = 5,
                intermediateSplines = 3
            ),
            LSystem(
                name = "KrishnaAnklets",
                angle = 45.0,
                rules = mapOf("X" to "XFX--XFX"),
                forwardChars = setOf("F"),
                axiom = "-X--X",
                lineWidthExp = 2.0,
                lineWidthBold = 0.15,
                minIterations = 1,
                maxIterations = 7,
                intermediateSplines = 0
            )
        )
    }
}