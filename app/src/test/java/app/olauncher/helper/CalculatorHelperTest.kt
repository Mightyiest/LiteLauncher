package app.olauncher.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorHelperTest {

    @Test
    fun `basic arithmetic and operator precedence`() {
        val r1 = CalculatorHelper.evaluate("2 + 3 * 4")
        assertNotNull(r1)
        assertEquals("14", r1?.formattedResult)
        assertEquals("14", r1?.rawResult)

        val r2 = CalculatorHelper.evaluate("(2 + 3) * 4")
        assertEquals("20", r2?.formattedResult)
    }

    @Test
    fun `unicode operators and comma grouping`() {
        val r1 = CalculatorHelper.evaluate("1,250 × 0.12")
        assertEquals("150", r1?.formattedResult)
        assertEquals("150", r1?.rawResult)

        val r2 = CalculatorHelper.evaluate("100 ÷ 4")
        assertEquals("25", r2?.formattedResult)
    }

    @Test
    fun `implicit multiplication with parentheses`() {
        assertEquals("14", CalculatorHelper.evaluate("2(3+4)")?.rawResult)
        assertEquals("25", CalculatorHelper.evaluate("(2+3)(4+1)")?.rawResult)
    }

    @Test
    fun `unary signs and exponents`() {
        assertEquals("7", CalculatorHelper.evaluate("-5 + 12")?.rawResult)
        assertEquals("-20", CalculatorHelper.evaluate("10 * -2")?.rawResult)

        val pow = CalculatorHelper.evaluate("2^10")
        assertEquals("1,024", pow?.formattedResult)
        assertEquals("1024", pow?.rawResult)

        assertEquals("512", CalculatorHelper.evaluate("2^3^2")?.rawResult)
        assertEquals("0.5", CalculatorHelper.evaluate("2^-1")?.rawResult)
    }

    @Test
    fun `percentage semantics`() {
        assertEquals("30", CalculatorHelper.evaluate("200 * 15%")?.rawResult)
        assertEquals("560", CalculatorHelper.evaluate("500 + 12%")?.rawResult)
        assertEquals("800", CalculatorHelper.evaluate("1000 - 20%")?.rawResult)
        assertEquals("0.5", CalculatorHelper.evaluate("50%")?.rawResult)
        assertEquals("130", CalculatorHelper.evaluate("100 + 200 * 15%")?.rawResult)
    }

    @Test
    fun `floating point rounding and formatting`() {
        assertEquals("0.3", CalculatorHelper.evaluate("0.1 + 0.2")?.rawResult)
        assertEquals("0.33333333", CalculatorHelper.evaluate("1 / 3")?.rawResult)
        assertEquals("0", CalculatorHelper.evaluate("-5 + 5")?.rawResult)
        val grouped = CalculatorHelper.evaluate("1000 + 250.5")
        assertEquals("1,250.5", grouped?.formattedResult)
        assertEquals("1250.5", grouped?.rawResult)
    }

    @Test
    fun `ignores plain numbers, app names, incomplete expressions, and invalid math`() {
        assertNull(CalculatorHelper.evaluate(""))
        assertNull(CalculatorHelper.evaluate("42"))
        assertNull(CalculatorHelper.evaluate("09171234567"))
        assertNull(CalculatorHelper.evaluate("1password"))
        assertNull(CalculatorHelper.evaluate("2fa"))
        assertNull(CalculatorHelper.evaluate("12x5"))
        assertNull(CalculatorHelper.evaluate("12+"))
        assertNull(CalculatorHelper.evaluate("(10+5"))
        assertNull(CalculatorHelper.evaluate("10/0"))
        assertNull(CalculatorHelper.evaluate("9^9^9"))
    }

    @Test
    fun `detects potential math input vs app name queries`() {
        assertTrue(CalculatorHelper.isPotentialMathInput("7"))
        assertTrue(CalculatorHelper.isPotentialMathInput("1250"))
        assertTrue(CalculatorHelper.isPotentialMathInput("1250 + "))
        assertTrue(CalculatorHelper.isPotentialMathInput("(10"))
        assertTrue(CalculatorHelper.isPotentialMathInput("=5+5"))
        assertTrue(CalculatorHelper.isPotentialMathInput("="))

        assertFalse(CalculatorHelper.isPotentialMathInput(""))
        assertFalse(CalculatorHelper.isPotentialMathInput(" "))
        assertFalse(CalculatorHelper.isPotentialMathInput("1p"))
        assertFalse(CalculatorHelper.isPotentialMathInput("7eleven"))
        assertFalse(CalculatorHelper.isPotentialMathInput("discord"))
        assertFalse(CalculatorHelper.isPotentialMathInput("2fa"))
    }

    @Test
    fun `supports optional leading equals sign`() {
        val result = CalculatorHelper.evaluate("= 1250 * 0.12")
        assertNotNull(result)
        assertEquals("150", result?.rawResult)
        assertEquals("150", result?.formattedResult)
    }
}
