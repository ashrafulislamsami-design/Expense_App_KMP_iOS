package com.expenseapp.salarytracker.shared.model

import kotlin.jvm.JvmInline

/**
 * Type-safe, exact financial representation using integer minor units (poisha / cents).
 *
 * STRICT INVARIANT: Floats and Doubles are NEVER used for storing or calculating money.
 * For example: ৳500.00 is stored as MinorUnits(50000L).
 * 100% portable across Android, iOS, JVM, and macOS.
 */
@JvmInline
value class MinorUnits(val amountInMinorUnits: Long) : Comparable<MinorUnits> {

    operator fun plus(other: MinorUnits): MinorUnits = MinorUnits(amountInMinorUnits + other.amountInMinorUnits)

    operator fun minus(other: MinorUnits): MinorUnits = MinorUnits(amountInMinorUnits - other.amountInMinorUnits)

    operator fun times(scalar: Long): MinorUnits = MinorUnits(amountInMinorUnits * scalar)

    operator fun div(divisor: Long): MinorUnits = MinorUnits(amountInMinorUnits / divisor)

    operator fun unaryMinus(): MinorUnits = MinorUnits(-amountInMinorUnits)

    override fun compareTo(other: MinorUnits): Int =
        amountInMinorUnits.compareTo(other.amountInMinorUnits)

    fun isZero(): Boolean = amountInMinorUnits == 0L
    fun isPositive(): Boolean = amountInMinorUnits > 0L
    fun isNegative(): Boolean = amountInMinorUnits < 0L

    /**
     * Formats the minor units into a human-readable currency string with currency symbol.
     * e.g., ৳1,250.50 or -৳50.00
     */
    fun format(currencySymbol: String = "৳", decimals: Int = 2): String {
        val isNeg = amountInMinorUnits < 0
        val absVal = if (isNeg) -amountInMinorUnits else amountInMinorUnits
        val divisor = when (decimals) {
            0 -> 1L
            1 -> 10L
            2 -> 100L
            3 -> 1000L
            else -> 100L
        }
        val whole = absVal / divisor
        val fraction = absVal % divisor

        val formattedWhole = formatWithCommas(whole)
        val fractionStr = if (decimals > 0) {
            "." + fraction.toString().padStart(decimals, '0')
        } else {
            ""
        }

        return if (isNeg) {
            "-$currencySymbol$formattedWhole$fractionStr"
        } else {
            "$currencySymbol$formattedWhole$fractionStr"
        }
    }

    fun formatCompact(currencySymbol: String = "৳"): String {
        return if (amountInMinorUnits % 100L == 0L) {
            format(currencySymbol, decimals = 2).removeSuffix(".00")
        } else {
            format(currencySymbol, decimals = 2)
        }
    }

    companion object {
        val ZERO = MinorUnits(0L)

        private fun formatWithCommas(value: Long): String {
            val s = value.toString()
            val len = s.length
            if (len <= 3) return s
            val sb = StringBuilder()
            var count = 0
            for (i in len - 1 downTo 0) {
                sb.append(s[i])
                count++
                if (count % 3 == 0 && i > 0) {
                    sb.append(',')
                }
            }
            return sb.reverse().toString()
        }

        /**
         * Parses a user-input decimal string (e.g. "1250.50" or "80") to exact MinorUnits.
         * Pure integer logic without Float/Double conversion.
         */
        fun fromDecimalString(input: String, decimals: Int = 2): MinorUnits {
            val clean = input.trim().replace(",", "")
            if (clean.isEmpty()) return ZERO

            val isNegative = clean.startsWith("-")
            val unsigned = if (isNegative) clean.removePrefix("-") else clean
            if (unsigned.isEmpty()) return ZERO

            val parts = unsigned.split('.')
            val wholePart = parts[0].toLongOrNull() ?: 0L
            val fractionPartRaw = if (parts.size > 1) parts[1] else ""

            val multiplier = when (decimals) {
                0 -> 1L
                1 -> 10L
                2 -> 100L
                3 -> 1000L
                else -> 100L
            }

            val fractionDigits = fractionPartRaw.take(decimals).padEnd(decimals, '0')
            val fractionPart = fractionDigits.toLongOrNull() ?: 0L

            val totalMinor = (wholePart * multiplier) + fractionPart
            return if (isNegative) MinorUnits(-totalMinor) else MinorUnits(totalMinor)
        }

        fun fromMajor(majorAmount: Long, decimals: Int = 2): MinorUnits {
            val multiplier = when (decimals) {
                0 -> 1L
                1 -> 10L
                2 -> 100L
                3 -> 1000L
                else -> 100L
            }
            return MinorUnits(majorAmount * multiplier)
        }
    }
}
