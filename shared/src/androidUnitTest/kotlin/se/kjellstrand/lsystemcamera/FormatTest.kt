package se.kjellstrand.lsystemcamera

import org.junit.Assert.assertEquals
import org.junit.Test

/** The slider labels lost String.format on the way to commonMain; these must still match it. */
class FormatTest {

    @Test
    fun format2MatchesStringFormatOverTheContrastRange() {
        var v = 0f
        while (v <= 1f) {
            assertEquals("%.2f".format(v), format2(v))
            v += 0.01f
        }
    }

    @Test
    fun formatSigned1MatchesStringFormatOverTheBrightnessRange() {
        var v = -2f
        while (v <= 2f) {
            assertEquals("%+.1f".format(v), formatSigned1(v))
            v += 0.1f
        }
    }
}
