package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.ReportingCycleMode
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.SalaryFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SalaryCycleCalculatorTest {

    private val calculator = SalaryCycleCalculator()

    @Test
    fun testCalendarMonthWindow() {
        val config = SalaryCycleConfig(reportingMode = ReportingCycleMode.CALENDAR_MONTH)
        val testDate = LocalDate.of(2026, 9, 15)
        val window = calculator.calculateWindow(testDate, config)

        assertEquals(LocalDate.of(2026, 9, 1), window.startDate)
        assertEquals(LocalDate.of(2026, 9, 30), window.endDate)
        assertEquals(30L, window.totalDays)
        assertTrue(window.contains(LocalDate.of(2026, 9, 1)))
        assertTrue(window.contains(LocalDate.of(2026, 9, 30)))
    }

    @Test
    fun testMonthlySalaryCycle_OnOrAfterPayday() {
        // Salary on 25th of month
        val config = SalaryCycleConfig(
            salaryDayOfMonth = 25,
            frequency = SalaryFrequency.MONTHLY,
            reportingMode = ReportingCycleMode.SALARY_CYCLE
        )

        // Date is 29 September 2026 (after payday 25 Sep)
        val testDate = LocalDate.of(2026, 9, 29)
        val window = calculator.calculateWindow(testDate, config)

        // Cycle should be 25 Sep 2026 to 24 Oct 2026
        assertEquals(LocalDate.of(2026, 9, 25), window.startDate)
        assertEquals(LocalDate.of(2026, 10, 24), window.endDate)
        assertTrue(window.contains(testDate))
    }

    @Test
    fun testMonthlySalaryCycle_BeforePayday() {
        // Salary on 25th of month
        val config = SalaryCycleConfig(
            salaryDayOfMonth = 25,
            frequency = SalaryFrequency.MONTHLY,
            reportingMode = ReportingCycleMode.SALARY_CYCLE
        )

        // Date is 10 September 2026 (before payday 25 Sep)
        val testDate = LocalDate.of(2026, 9, 10)
        val window = calculator.calculateWindow(testDate, config)

        // Cycle should be 25 Aug 2026 to 24 Sep 2026
        assertEquals(LocalDate.of(2026, 8, 25), window.startDate)
        assertEquals(LocalDate.of(2026, 9, 24), window.endDate)
        assertTrue(window.contains(testDate))
    }

    @Test
    fun testSalaryCycle_MonthBoundary31stInFebruary() {
        // Configured payday is 31st of every month
        val config = SalaryCycleConfig(
            salaryDayOfMonth = 31,
            frequency = SalaryFrequency.MONTHLY,
            reportingMode = ReportingCycleMode.SALARY_CYCLE
        )

        // Test in February 2026 (non-leap year, 28 days)
        val testDate = LocalDate.of(2026, 2, 10)
        val window = calculator.calculateWindow(testDate, config)

        // Previous payday was Jan 31. Current payday in Feb is clamped to Feb 28.
        // Cycle is Jan 31 to Feb 27.
        assertEquals(LocalDate.of(2026, 1, 31), window.startDate)
        assertEquals(LocalDate.of(2026, 2, 27), window.endDate)
    }

    @Test
    fun testSalaryCycle_LeapYearHandling() {
        // Configured payday is 29th of every month
        val config = SalaryCycleConfig(
            salaryDayOfMonth = 29,
            frequency = SalaryFrequency.MONTHLY,
            reportingMode = ReportingCycleMode.SALARY_CYCLE
        )

        // 2024 is a leap year (Feb 29 exists)
        val leapFeb = LocalDate.of(2024, 2, 29)
        val windowLeap = calculator.calculateWindow(leapFeb, config)
        assertEquals(LocalDate.of(2024, 2, 29), windowLeap.startDate)
        assertEquals(LocalDate.of(2024, 3, 28), windowLeap.endDate)

        // 2025 is non-leap (Feb 28 max)
        val nonLeapFeb = LocalDate.of(2025, 2, 15)
        val windowNonLeap = calculator.calculateWindow(nonLeapFeb, config)
        // Previous was Jan 29, next was clamped to Feb 28 -> end is Feb 27
        assertEquals(LocalDate.of(2025, 1, 29), windowNonLeap.startDate)
        assertEquals(LocalDate.of(2025, 2, 27), windowNonLeap.endDate)
    }
}
