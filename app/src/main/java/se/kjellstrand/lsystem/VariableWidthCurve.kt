package se.kjellstrand.lsystem

import se.kjellstrand.lsystem.model.LSTriple
import kotlin.math.*

var leftHull = mutableListOf<LSTriple>()
var rightHull = mutableListOf<LSTriple>()

fun buildHullFromPolygon(ppList: List<LSTriple>): MutableList<LSTriple> {
    // Only create new left and right hulls if(leftHull.size != ppList.size -1)
    if (leftHull.size != ppList.size - 1) {
        leftHull = mutableListOf()
        rightHull = mutableListOf()
        // Initialize the left and right hull lists
        for (i in 0 until ppList.size-2) {
            leftHull.add(LSTriple(0.0, 0.0, 1.0))
            rightHull.add(LSTriple(0.0, 0.0, 1.0))
        }
    }

    for (i in 1 until ppList.size - 1) {
        val p0 = ppList[i - 1]
        val p1 = ppList[i]

        val (a, b, c) = calculateSidesOfTriangle(p0, p1)

        val alfaPlus90 = calculateAlfa(a, c, b, (PI / 2.0))
        val alfaMinus90 = calculateAlfa(a, c, b, -(PI / 2.0))

        calculatePerpendicularPolyPoint(leftHull[i - 1], p0, p1, alfaPlus90)
        calculatePerpendicularPolyPoint(rightHull[i - 1], p0, p1, alfaMinus90)
    }

    val hull = mutableListOf<LSTriple>()
    hull.addAll(leftHull)
    hull.addAll(rightHull.reversed())
    return hull
}

private fun calculatePerpendicularPolyPoint(
    result: LSTriple,
    p0: LSTriple,
    p1: LSTriple,
    alfaPlus90: Double
) {
    val width = ((p0.w + p1.w) / 2.0)
    val x = (p0.x + p1.x) / 2.0 + width * sin(alfaPlus90)
    val y = (p0.y + p1.y) / 2.0 + width * cos(alfaPlus90)
    result.x = x
    result.y = y
}

private fun calculateSidesOfTriangle(
    p0: LSTriple,
    p1: LSTriple
): Triple<Double, Double, Double> {
    val a = p0.x - p1.x
    val b = p0.y - p1.y
    val c = sqrt(a.pow(2.0) + b.pow(2.0))
    return Triple(a, b, c)
}

private fun calculateAlfa(a: Double, c: Double, b: Double, angle: Double): Double {
    val alfa = asin(a / c)
    var alfaPlus90 = alfa + angle
    // Take care of special case when adjacent side is negative
    if (b < 0) alfaPlus90 = -alfaPlus90
    return alfaPlus90
}

fun getMidPoint(
    p0: LSTriple,
    p1: LSTriple
): LSTriple {
    return LSTriple((p0.x + p1.x) / 2.0, (p0.y + p1.y) / 2.0, (p0.w + p1.w) / 2.0)
}