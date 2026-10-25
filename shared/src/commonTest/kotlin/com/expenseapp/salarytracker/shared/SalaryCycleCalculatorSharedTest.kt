package com.expenseapp.salarytracker.shared

import com.expenseapp.salarytracker.shared.model.FinancialDate
import com.expenseapp.salarytracker.shared.model.ReportingCycleMode
import com.expenseapp.salarytracker.shared.model.SalaryCycleConfig
import com.expenseapp.salarytracker.shared.model.SalaryFrequency
import com.expenseapp.salarytracker.shared.service.SalaryCycleCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SalaryCycleCalculatorSharedTest {

    private val calculator = SalaryCycleCalculator()

    @Test
    fun testCalendarMonthWindow() {
        val config = SalaryCycleConfig(reportingMode = ReportingCycleMode.CALENDAR_MONTH)
        val testDate = FinancialDate(2026, 9, 15)
        val window = calculator.calculateWindow(testDate, config)

        assertEquals(FinancialDate(2026, 9, 1), window.startDate)
        assertEquals(FinancialDate(2026, 9, 30), window.endDate)
        assertEquals(30L, window.totalDays)
        assertTrue(window.contains(FinancialDate(2026, 9, 1)))
        assertTrue(window.contains(FinancialDate(2026, 9, 30)))
    }

    @Test
    fun testMonthlySalaryCycle_OnOrAfterPayday() {
        val config = SalaryCycleConfig(
            salaryDayOfMonth = 25,
            frequency = SalaryFrequency.MONTHLY,
            reportingMode = ReportingCycleMode.SALARY_CYCLE
        )

        // Date is 29 September 2026 (after payday 25 Sep)
        val testDate = FinancialDate(2026, 9, 29)
        val window = calculator.calculateWindow(testDate, config)

        // Cycle should be 25 Sep 2026 to 24 Oct 2026
        assertEquals(FinancialDate(2026, 9, 25), window.startDate)
        assertEquals(FinancialDate(2026, 10, 24), window.endDate)
        assertTrue(window.contains(testDate))
    }

    @Test
    fun testMonthlySalaryCycle_BeforePayday() {
        val config = SalaryCycleConfig(
            salaryDayOfMonth = 25,
            frequency = SalaryFrequency.MONTHLY,
            reportingMode = ReportingCycleMode.SALARY_CYCLE
        )

        // Date is 10 September 2026 (before payday 25 Sep)
        val testDate = FinancialDate(2026, 9, 10)
        val window = calculator.calculateWindow(testDate, config)

        // Cycle should be 25 Aug 2026 to 24 Sep 2026
        assertEquals(FinancialDate(2026, 8, 25), window.startDate)
        assertEquals(FinancialDate(2026, 9, 24), window.endDate)
        assertTrue(window.contains(testDate))
    }

    @Test
    fun testSalaryCycle_MonthBoundary31stInFebruary() {
        val config = SalaryCycleConfig(
            salaryDayOfMonth = 31,
            frequency = SalaryFrequency.MONTHLY,
            reportingMode = ReportingCycleMode.SALARY_CYCLE
        )

        // Test in February 2026 (non-leap year, 28 days)
        val testDate = FinancialDate(2026, 2, 10)
        val window = calculator.calculateWindow(testDate, config)

        // Previous payday was Jan 31. Current payday in Feb is clamped to Feb 28.
        // Cycle is Jan 31 to Feb 27.
        assertEquals(FinancialDate(2026, 1, 31), window.startDate)
        assertEquals(FinancialDate(2026, 2, 27), window.endDate)
    }

    @Test
    fun testSalaryCycle_LeapYearHandling() {
        val config = SalaryCycleConfig(
            salaryDayOfMonth = 29,
            frequency = SalaryFrequency.MONTHLY,
            reportingMode = ReportingCycleMode.SALARY_CYCLE
        )

        // 2024 is a leap year (Feb 29 exists)
        val leapFeb = FinancialDate(2024, 2, 29)
        val windowLeap = calculator.calculateWindow(leapFeb, config)
        assertEquals(FinancialDate(2024, 2, 29), windowLeap.startDate)
        assertEquals(FinancialDate(2024, 3, 28), windowLeap.endDate)

        // 2025 is non-leap (Feb 28 max)
        val nonLeapFeb = FinancialDate(2025, 2, 15)
        val windowNonLeap = calculator.calculateWindow(nonLeapFeb, config)
        assertEquals(FinancialDate(2025, 1, 29), windowNonLeap.startDate)
        assertEquals(FinancialDate(2025, 2, 27), windowNonLeap.endDate)
    }
}
