package com.expenseapp.salarytracker.shared.service

import com.expenseapp.salarytracker.shared.model.FinancialDate
import com.expenseapp.salarytracker.shared.model.ReportingCycleMode
import com.expenseapp.salarytracker.shared.model.SalaryCycleConfig
import com.expenseapp.salarytracker.shared.model.SalaryFrequency

data class DateWindow(
    val startDate: FinancialDate,
    val endDate: FinancialDate
) {
    val totalDays: Long get() = FinancialDate.daysBetween(startDate, endDate) + 1

    fun contains(date: FinancialDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)

    fun remainingDays(from: FinancialDate): Long {
        if (from.isAfter(endDate)) return 0
        val effectiveStart = if (from.isBefore(startDate)) startDate else from
        return FinancialDate.daysBetween(effectiveStart, endDate) + 1
    }
}

class SalaryCycleCalculator {

    /**
     * Calculates the deterministic cycle window for a given date based on user configuration.
     * Guaranteed leap year and month-end safe.
     */
    fun calculateWindow(targetDate: FinancialDate, config: SalaryCycleConfig): DateWindow {
        return when (config.reportingMode) {
            ReportingCycleMode.CALENDAR_MONTH -> {
                val start = targetDate.withDayOfMonth(1)
                val end = targetDate.withDayOfMonth(targetDate.lengthOfMonth)
                DateWindow(start, end)
            }
            ReportingCycleMode.SALARY_CYCLE -> {
                when (config.frequency) {
                    SalaryFrequency.MONTHLY -> calculateMonthlySalaryWindow(targetDate, config.salaryDayOfMonth)
                    SalaryFrequency.BI_WEEKLY -> calculateBiWeeklyWindow(targetDate, config.anchorDate)
                    SalaryFrequency.WEEKLY -> calculateWeeklyWindow(targetDate, config.anchorDate)
                    SalaryFrequency.CUSTOM -> calculateCustomWindow(targetDate, config.anchorDate, config.customCycleLengthDays)
                }
            }
        }
    }

    private fun calculateMonthlySalaryWindow(targetDate: FinancialDate, configuredDay: Int): DateWindow {
        val safeConfigDay = configuredDay.coerceIn(1, 31)

        val targetMonthLength = targetDate.lengthOfMonth
        val currentMonthPayday = targetDate.withDayOfMonth(minOf(safeConfigDay, targetMonthLength))

        return if (!targetDate.isBefore(currentMonthPayday)) {
            // Target is on or after payday in current month -> cycle goes from current month payday to day before next month payday
            val nextMonth = targetDate.plusMonths(1)
            val nextMonthPayday = nextMonth.withDayOfMonth(minOf(safeConfigDay, nextMonth.lengthOfMonth))
            DateWindow(currentMonthPayday, nextMonthPayday.minusDays(1))
        } else {
            // Target is before payday in current month -> cycle goes from previous month payday to day before current month payday
            val prevMonth = targetDate.minusMonths(1)
            val prevMonthPayday = prevMonth.withDayOfMonth(minOf(safeConfigDay, prevMonth.lengthOfMonth))
            DateWindow(prevMonthPayday, currentMonthPayday.minusDays(1))
        }
    }

    private fun calculateBiWeeklyWindow(targetDate: FinancialDate, anchorDate: FinancialDate): DateWindow {
        val daysDiff = FinancialDate.daysBetween(anchorDate, targetDate)
        val cycleIndex = floorDiv(daysDiff, 14L)
        val start = anchorDate.plusDays(cycleIndex * 14L)
        val end = start.plusDays(13L)
        return DateWindow(start, end)
    }

    private fun calculateWeeklyWindow(targetDate: FinancialDate, anchorDate: FinancialDate): DateWindow {
        val daysDiff = FinancialDate.daysBetween(anchorDate, targetDate)
        val cycleIndex = floorDiv(daysDiff, 7L)
        val start = anchorDate.plusDays(cycleIndex * 7L)
        val end = start.plusDays(6L)
        return DateWindow(start, end)
    }

    private fun calculateCustomWindow(targetDate: FinancialDate, anchorDate: FinancialDate, cycleLengthDays: Int): DateWindow {
        val len = cycleLengthDays.coerceAtLeast(1).toLong()
        val daysDiff = FinancialDate.daysBetween(anchorDate, targetDate)
        val cycleIndex = floorDiv(daysDiff, len)
        val start = anchorDate.plusDays(cycleIndex * len)
        val end = start.plusDays(len - 1)
        return DateWindow(start, end)
    }

    private fun floorDiv(x: Long, y: Long): Long {
        var r = x / y
        if ((x xor y) < 0 && (r * y != x)) {
            r--
        }
        return r
    }
}
