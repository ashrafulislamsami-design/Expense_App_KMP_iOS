package com.expenseapp.salarytracker.shared.model

/**
 * Lightweight, zero-dependency multiplatform date representation.
 *
 * Implements Proleptic Gregorian calendar math, leap year calculations,
 * and exact day-difference arithmetic without Java-specific or iOS-specific imports.
 */
data class FinancialDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<FinancialDate> {

    init {
        require(month in 1..12) { "Month must be between 1 and 12: $month" }
        require(day in 1..daysInMonth(year, month)) { "Day $day is invalid for month $month in year $year" }
    }

    override fun compareTo(other: FinancialDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun isBefore(other: FinancialDate): Boolean = compareTo(other) < 0
    fun isAfter(other: FinancialDate): Boolean = compareTo(other) > 0

    val lengthOfMonth: Int get() = daysInMonth(year, month)

    fun withDayOfMonth(newDay: Int): FinancialDate {
        val safeDay = newDay.coerceIn(1, lengthOfMonth)
        return FinancialDate(year, month, safeDay)
    }

    fun plusMonths(monthsToAdd: Int): FinancialDate {
        if (monthsToAdd == 0) return this
        val totalMonths = (year * 12 + (month - 1)) + monthsToAdd
        val newYear = totalMonths / 12
        val newMonth = (totalMonths % 12) + 1
        val maxDays = daysInMonth(newYear, newMonth)
        val safeDay = minOf(day, maxDays)
        return FinancialDate(newYear, newMonth, safeDay)
    }

    fun minusMonths(monthsToSubtract: Int): FinancialDate = plusMonths(-monthsToSubtract)

    fun toEpochDays(): Long {
        var y = year.toLong()
        var m = month.toLong()
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m - 3) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    fun plusDays(daysToAdd: Long): FinancialDate {
        if (daysToAdd == 0L) return this
        return fromEpochDays(toEpochDays() + daysToAdd)
    }

    fun minusDays(daysToSubtract: Long): FinancialDate = plusDays(-daysToSubtract)

    fun toIsoString(): String {
        val mStr = month.toString().padStart(2, '0')
        val dStr = day.toString().padStart(2, '0')
        return "$year-$mStr-$dStr"
    }

    override fun toString(): String = toIsoString()

    companion object {
        fun isLeapYear(year: Int): Boolean =
            (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

        fun daysInMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        fun daysBetween(start: FinancialDate, end: FinancialDate): Long =
            end.toEpochDays() - start.toEpochDays()

        fun fromEpochDays(epochDays: Long): FinancialDate {
            val z = epochDays + 719468
            val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            var y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
            val m = (mp + if (mp < 10) 3 else -9).toInt()
            if (m <= 2) y += 1
            return FinancialDate(y.toInt(), m, d)
        }

        fun parse(isoString: String): FinancialDate {
            val parts = isoString.trim().split('-')
            require(parts.size == 3) { "Invalid ISO date string: $isoString" }
            return FinancialDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }

        fun of(year: Int, month: Int, day: Int): FinancialDate =
            FinancialDate(year, month, day)
    }
}
