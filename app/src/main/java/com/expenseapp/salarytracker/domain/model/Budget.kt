package com.expenseapp.salarytracker.domain.model

import java.time.LocalDate

enum class BudgetPeriod {
    MONTHLY,
    SALARY_CYCLE,
    CUSTOM;

    companion object {
        fun fromString(value: String?): BudgetPeriod {
            if (value == null) return SALARY_CYCLE
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: SALARY_CYCLE
        }
    }
}

data class Budget(
    val id: Long = 0,
    val categoryId: Long? = null, // null means overall budget
    val limitAmount: MinorUnits,
    val period: BudgetPeriod = BudgetPeriod.SALARY_CYCLE,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val warningThresholdPercentage: Int = 80 // Default warning at 80%
)
