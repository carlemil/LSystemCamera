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
    /** Iteration shown when the curve is picked: dense enough to carry an image, not a black square. */
    val defaultIterations: Int = maxIterations - 1,
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
                lineWidthExp = 1.414,
                lineWidthBold = 0.128,
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
                lineWidthExp = 1.414,
                lineWidthBold = 0.116,
                minIterations = 1,
                maxIterations = 12,
                intermediateSplines = 3
            ),
            LSystem(
                name = "Fudgeflake",
                angle = 30.0,
                rules = mapOf("F" to "+F----F++++F-"),
                forwardChars = setOf("F"),
                axiom = "F++++F++++F",
                lineWidthExp = 1.732,
                lineWidthBold = 0.165,
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
                lineWidthBold = 0.385,
                minIterations = 1,
                maxIterations = 8,
                defaultIterations = 6,
                intermediateSplines = 0
            ),
            LSystem(
                name = "SierpinskiTriangle",
                angle = 60.0,
                rules = mapOf("A" to "BF-AF-B", "B" to "AF+BF+A"),
                forwardChars = setOf("F"),
                axiom = "A",
                lineWidthExp = 2.0,
                lineWidthBold = 0.383,
                minIterations = 1,
                maxIterations = 9,
                defaultIterations = 7,
                intermediateSplines = 0
            ),
            LSystem(
                name = "SierpinskiCurve",
                angle = 45.0,
                rules = mapOf("X" to "XF+G+XF--F--XF+G+X"),
                forwardChars = setOf("F", "G"),
                axiom = "F--XF--F--XF",
                lineWidthExp = 2.0,
                lineWidthBold = 0.114,
                minIterations = 1,
                maxIterations = 7,
                defaultIterations = 5,
                intermediateSplines = 0
            ),
            LSystem(
                name = "SierpinskiSquare",
                angle = 90.0,
                rules = mapOf("X" to "XF-F+F-XF+F+XF-F+F-X"),
                forwardChars = setOf("F"),
                axiom = "F+XF+F+XF",
                lineWidthExp = 2.0,
                lineWidthBold = 0.098,
                minIterations = 1,
                maxIterations = 7,
                defaultIterations = 5,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Gosper",
                angle = 60.0,
                rules = mapOf("A" to "A-B--B+A++AA+B-", "B" to "+A-BB--B-A++A+B"),
                forwardChars = setOf("A", "B"),
                axiom = "A",
                lineWidthExp = 2.646,
                lineWidthBold = 0.326,
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
                lineWidthBold = 0.385,
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
                lineWidthBold = 0.193,
                minIterations = 1,
                maxIterations = 7,
                defaultIterations = 5,
                intermediateSplines = 0
            ),
            LSystem(
                name = "QuadraticGosper",
                angle = 90.0,
                rules = mapOf("X" to "XFX-YF-YF+FX+FX-YF-YFFX+YF+FXFXYF-FX+YF+FXFX+YF-FXYF-YF-FX+FX+YFYF-",
                    "Y" to "+FXFX-YF-YF+FX+FXYF+FX-YFYF-FX-YF+FXYFYF-FX-YFFX+FX+YF-YF-FX+FX+YFY"),
                forwardChars = setOf("F"),
                axiom = "-YF",
                lineWidthExp = 5.0,
                lineWidthBold = 0.372,
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
                lineWidthExp = 2.25,
                lineWidthBold = 0.113,
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
                lineWidthExp = 2.618,
                lineWidthBold = 0.146,
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
                lineWidthBold = 0.503,
                minIterations = 1,
                maxIterations = 5,
                intermediateSplines = 3
            ),
            LSystem(
                name = "FassThree",
                angle = 90.0,
                rules = mapOf("L" to "LF+RFR+FL-F-LFLFL-FRFR+", "R" to "-LFLF+RFRFR+F+RF-LFL-FR"),
                forwardChars = setOf("F"),
                axiom = "-L",
                lineWidthExp = 3.0,
                lineWidthBold = 0.35,
                minIterations = 1,
                maxIterations = 5,
                intermediateSplines = 0
            ),
            LSystem(
                name = "FassFour",
                angle = 90.0,
                rules = mapOf("L" to "LFLF+RFR+FLFL-FRF-LFL-FR+F+RF-LFL-FRFRFR+", "R" to "-LFLFLF+RFR+FL-F-LF+RFR+FLF+RFRF-LFL-FRFR"),
                forwardChars = setOf("F"),
                axiom = "-L",
                lineWidthExp = 4.0,
                lineWidthBold = 0.4,
                minIterations = 1,
                maxIterations = 4,
                intermediateSplines = 0
            ),
            LSystem(
                name = "Terdragon",
                angle = 120.0,
                rules = mapOf("F" to "F+F-F"),
                forwardChars = setOf("F"),
                axiom = "F",
                lineWidthExp = 1.732,
                lineWidthBold = 0.198,
                minIterations = 1,
                maxIterations = 10,
                defaultIterations = 8,
                intermediateSplines = 1
            ),
            LSystem(
                name = "KrishnaAnklets",
                angle = 45.0,
                rules = mapOf("X" to "XFX--XFX"),
                forwardChars = setOf("F"),
                axiom = "-X--X",
                lineWidthExp = 2.0,
                lineWidthBold = 0.273,
                minIterations = 1,
                maxIterations = 7,
                intermediateSplines = 0
            )
        )
    }
}