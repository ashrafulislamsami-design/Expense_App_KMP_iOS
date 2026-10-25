package com.expenseapp.salarytracker.domain.model.report

import java.time.LocalDate

enum class ReportPeriodType(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CURRENT_SALARY_CYCLE("Current Salary Cycle"),
    PREVIOUS_SALARY_CYCLE("Previous Salary Cycle"),
    CUSTOM("Custom Range")
}

data class ReportDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val periodType: ReportPeriodType,
    val label: String
) {
    init {
        require(!startDate.isAfter(endDate)) {
            "Report start date ($startDate) must not be after end date ($endDate)"
        }
    }
}
