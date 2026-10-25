package com.expenseapp.salarytracker.shared.model

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
    val categoryId: Long? = null,
    val limitAmount: MinorUnits,
    val period: BudgetPeriod = BudgetPeriod.SALARY_CYCLE,
    val startDate: FinancialDate? = null,
    val endDate: FinancialDate? = null,
    val warningThresholdPercentage: Int = 80
)
