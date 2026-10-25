package com.expenseapp.salarytracker.domain.model

import java.time.LocalDate

enum class SalaryFrequency(val displayName: String) {
    MONTHLY("Monthly"),
    BI_WEEKLY("Bi-weekly (Every 2 weeks)"),
    WEEKLY("Weekly"),
    CUSTOM("Custom Cycle");

    companion object {
        fun fromString(value: String): SalaryFrequency =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
    }
}

enum class ReportingCycleMode(val displayName: String) {
    CALENDAR_MONTH("Calendar Month (1st to Month End)"),
    SALARY_CYCLE("Salary Cycle (Payday to Next Payday)");

    companion object {
        fun fromString(value: String): ReportingCycleMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SALARY_CYCLE
    }
}

data class SalaryCycleConfig(
    val salaryAmount: MinorUnits = MinorUnits.fromMajor(50000L), // Default ৳50,000
    val currency: String = "BDT",
    val salaryDayOfMonth: Int = 25, // Default 25th of month
    val frequency: SalaryFrequency = SalaryFrequency.MONTHLY,
    val reportingMode: ReportingCycleMode = ReportingCycleMode.SALARY_CYCLE,
    val anchorDate: LocalDate = LocalDate.now(),
    val customCycleLengthDays: Int = 30
)
