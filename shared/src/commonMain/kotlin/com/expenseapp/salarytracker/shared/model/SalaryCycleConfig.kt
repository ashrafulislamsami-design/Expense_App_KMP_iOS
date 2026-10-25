package com.expenseapp.salarytracker.shared.model

enum class SalaryFrequency {
    MONTHLY,
    BI_WEEKLY,
    WEEKLY,
    CUSTOM;

    companion object {
        fun fromString(value: String?): SalaryFrequency {
            if (value == null) return MONTHLY
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: MONTHLY
        }
    }
}

enum class ReportingCycleMode {
    CALENDAR_MONTH,
    SALARY_CYCLE;

    companion object {
        fun fromString(value: String?): ReportingCycleMode {
            if (value == null) return SALARY_CYCLE
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: SALARY_CYCLE
        }
    }
}

data class SalaryCycleConfig(
    val salaryAmount: MinorUnits = MinorUnits(5000000L), // Default ৳50,000.00
    val currency: String = "BDT",
    val salaryDayOfMonth: Int = 1,
    val frequency: SalaryFrequency = SalaryFrequency.MONTHLY,
    val reportingMode: ReportingCycleMode = ReportingCycleMode.SALARY_CYCLE,
    val anchorDate: FinancialDate = FinancialDate(2026, 1, 1),
    val customCycleLengthDays: Int = 30
)
