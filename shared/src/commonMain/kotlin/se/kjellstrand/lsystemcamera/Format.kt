package se.kjellstrand.lsystemcamera

import kotlin.math.abs
import kotlin.math.roundToInt

/** `String.format` is JVM-only; these two cover the slider labels. */

/** 0.5f -> "0.50". Non-negative values only. */
internal fun format2(value: Float): String {
    val n = (value * 100).roundToInt()
    return "${n / 100}.${(n % 100).toString().padStart(2, '0')}"
}

/** -1.5f -> "-1.5", 0f -> "+0.0". */
internal fun formatSigned1(value: Float): String {
    val n = (value * 10).roundToInt()
    val a = abs(n)
    return "${if (n < 0) "-" else "+"}${a / 10}.${a % 10}"
}
