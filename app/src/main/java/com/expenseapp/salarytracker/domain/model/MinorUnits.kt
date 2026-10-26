package com.expenseapp.salarytracker.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Type-safe, exact financial representation using integer minor units (poisha / cents).
 *
 * STRICT INVARIANT: Floats and Doubles are NEVER used for storing or calculating money.
 * For example: ৳500.00 is stored as MinorUnits(50000L).
 */
@JvmInline
value class MinorUnits(val amountInMinorUnits: Long) : Comparable<MinorUnits> {

    operator fun plus(other: MinorUnits): MinorUnits = MinorUnits(amountInMinorUnits + other.amountInMinorUnits)

    operator fun minus(other: MinorUnits): MinorUnits = MinorUnits(amountInMinorUnits - other.amountInMinorUnits)

    operator fun times(scalar: Long): MinorUnits = MinorUnits(amountInMinorUnits * scalar)

    operator fun unaryMinus(): MinorUnits = MinorUnits(-amountInMinorUnits)

    fun toBigDecimal(decimals: Int = 2): BigDecimal =
        BigDecimal.valueOf(amountInMinorUnits, decimals)

    override fun compareTo(other: MinorUnits): Int =
        amountInMinorUnits.compareTo(other.amountInMinorUnits)

    fun isZero(): Boolean = amountInMinorUnits == 0L
    fun isPositive(): Boolean = amountInMinorUnits > 0L
    fun isNegative(): Boolean = amountInMinorUnits < 0L

    /**
     * Formats the minor units into a human-readable currency string with currency symbol.
     * e.g., ৳1,250.50 or ($50.00)
     */
    fun format(currencySymbol: String = "\u09F3", decimals: Int = 2): String {
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

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val wholeFormatter = DecimalFormat("#,##0", symbols)
        val formattedWhole = wholeFormatter.format(whole)

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

    companion object {
        val ZERO = MinorUnits(0L)

        /**
         * Parses a user-input decimal string (e.g. "1250.50" or "80") to exact MinorUnits.
         * Never loses precision.
         */
        fun fromDecimalString(input: String, decimals: Int = 2): MinorUnits {
            val clean = input.trim().replace(",", "")
            if (clean.isEmpty()) return ZERO
            val bigDecimal = try {
                BigDecimal(clean)
            } catch (e: Exception) {
                BigDecimal.ZERO
            }
            val scaled = bigDecimal.setScale(decimals, RoundingMode.HALF_EVEN)
            return MinorUnits(scaled.unscaledValue().toLong())
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
