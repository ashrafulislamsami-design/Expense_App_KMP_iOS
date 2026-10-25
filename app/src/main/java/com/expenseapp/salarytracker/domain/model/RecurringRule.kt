package com.expenseapp.salarytracker.domain.model

import java.time.LocalDate

enum class RecurrenceFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BI_WEEKLY("Bi-weekly (Every 2 Weeks)"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom Days");

    companion object {
        fun fromString(value: String): RecurrenceFrequency =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
    }
}

data class RecurringRule(
    val id: Long = 0,
    val name: String,
    val amount: MinorUnits,
    val type: TransactionType = TransactionType.EXPENSE,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val intervalDays: Int = 30, // For CUSTOM frequency
    val preferredDayOfMonth: Int? = null, // e.g. 31 for month-end target
    val startDate: LocalDate,
    val nextDueDate: LocalDate,
    val endDate: LocalDate? = null,
    val autoPost: Boolean = false,
    val isPaused: Boolean = false,
    val note: String = ""
)
