package app.olauncher.helper

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

object CalculatorHelper {

    data class CalculationResult(
        val expression: String,
        val formattedResult: String,
        val rawResult: String,
    )

    private data class ParsedValue(
        val value: Double,
        val isPercentage: Boolean = false,
    )

    private val allowedCharsRegex = Regex("""^[0-9.,+\-*×/÷%^()\s]+$""")
    private val operatorChars = setOf('+', '-', '*', '×', '/', '÷', '%', '^')

    fun isPotentialMathInput(query: String): Boolean {
        val rawTrimmed = query.trim()
        if (rawTrimmed.isEmpty()) return false
        if (rawTrimmed == "=") return true
        val stripped = rawTrimmed.removePrefix("=").trim()
        return stripped.isNotEmpty() && allowedCharsRegex.matches(stripped)
    }

    fun evaluate(query: String): CalculationResult? {
        val trimmed = query.trim().removePrefix("=").trim()
        if (trimmed.isEmpty()) return null
        if (!allowedCharsRegex.matches(trimmed)) return null
        if (!trimmed.any { it.isDigit() }) return null
        if (!trimmed.any { it in operatorChars }) return null

        val normalized = trimmed
            .replace(" ", "")
            .replace("\t", "")
            .replace(",", "")
            .replace('×', '*')
            .replace('÷', '/')

        val value = try {
            Parser(normalized).parse()
        } catch (_: Exception) {
            return null
        } ?: return null

        if (!value.isFinite()) return null
        val cleanDouble = if (value == 0.0) 0.0 else value
        val (formatted, raw) = formatNumber(cleanDouble)
        return CalculationResult(
            expression = trimmed,
            formattedResult = formatted,
            rawResult = raw
        )
    }

    private fun formatNumber(value: Double): Pair<String, String> {
        if (abs(value) >= 1e15 || (value != 0.0 && abs(value) < 1e-6)) {
            val sci = BigDecimal.valueOf(value)
                .round(MathContext(8, RoundingMode.HALF_UP))
                .stripTrailingZeros()
                .toString()
            return sci to sci
        }
        val rounded = BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP)
        val normalized = if (rounded.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            rounded.stripTrailingZeros()
        }
        val raw = normalized.toPlainString()
        val formatter = DecimalFormat("#,##0.########", DecimalFormatSymbols(Locale.US))
        val formatted = formatter.format(normalized)
        return formatted to raw
    }

    private class Parser(private val input: String) {
        private var pos = 0

        fun parse(): Double? {
            val result = parseExpression()
            if (pos != input.length) return null
            return result.value
        }

        private fun peek(): Char? = if (pos < input.length) input[pos] else null

        private fun parseExpression(): ParsedValue {
            var left = parseTerm()
            while (true) {
                when (peek()) {
                    '+' -> {
                        pos++
                        val right = parseTerm()
                        val sum = if (right.isPercentage) {
                            left.value + (left.value * right.value)
                        } else {
                            left.value + right.value
                        }
                        left = ParsedValue(sum, isPercentage = false)
                    }

                    '-' -> {
                        pos++
                        val right = parseTerm()
                        val diff = if (right.isPercentage) {
                            left.value - (left.value * right.value)
                        } else {
                            left.value - right.value
                        }
                        left = ParsedValue(diff, isPercentage = false)
                    }

                    else -> return left
                }
            }
        }

        private fun parseTerm(): ParsedValue {
            var left = parseUnary()
            while (true) {
                when (peek()) {
                    '*' -> {
                        pos++
                        val right = parseUnary()
                        left = ParsedValue(left.value * right.value, isPercentage = false)
                    }

                    '/' -> {
                        pos++
                        val right = parseUnary()
                        if (right.value == 0.0) throw ArithmeticException("Division by zero")
                        left = ParsedValue(left.value / right.value, isPercentage = false)
                    }

                    '(' -> {
                        val right = parseUnary()
                        left = ParsedValue(left.value * right.value, isPercentage = false)
                    }

                    else -> return left
                }
            }
        }

        private fun parseUnary(): ParsedValue {
            return when (peek()) {
                '+' -> {
                    pos++
                    parseUnary()
                }

                '-' -> {
                    pos++
                    val operand = parseUnary()
                    ParsedValue(-operand.value, isPercentage = false)
                }

                else -> parsePower()
            }
        }

        private fun parsePower(): ParsedValue {
            val base = parseFactor()
            if (peek() == '^') {
                pos++
                val exponent = parseUnary()
                val result = base.value.pow(exponent.value)
                if (!result.isFinite()) throw ArithmeticException("Non-finite power result")
                return ParsedValue(result, isPercentage = false)
            }
            return base
        }

        private fun parseFactor(): ParsedValue {
            var result = when (peek()) {
                '(' -> {
                    pos++
                    val inner = parseExpression()
                    if (peek() != ')') throw IllegalArgumentException("Unclosed parenthesis")
                    pos++
                    ParsedValue(inner.value, isPercentage = false)
                }

                else -> parseNumber()
            }

            while (peek() == '%') {
                pos++
                result = ParsedValue(result.value / 100.0, isPercentage = true)
            }
            return result
        }

        private fun parseNumber(): ParsedValue {
            val start = pos
            var hasDigit = false
            var dotCount = 0
            while (pos < input.length) {
                val ch = input[pos]
                if (ch.isDigit()) {
                    hasDigit = true
                    pos++
                } else if (ch == '.') {
                    dotCount++
                    if (dotCount > 1) throw IllegalArgumentException("Invalid decimal")
                    pos++
                } else {
                    break
                }
            }
            if (!hasDigit) throw IllegalArgumentException("Expected number at $start")
            val number = input.substring(start, pos).toDouble()
            return ParsedValue(number, isPercentage = false)
        }
    }
}
