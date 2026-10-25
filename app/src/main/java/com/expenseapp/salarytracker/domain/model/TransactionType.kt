package com.expenseapp.salarytracker.domain.model

enum class TransactionType(val displayName: String, val isExpenseLike: Boolean) {
    EXPENSE("Expense", true),
    INCOME("Money received", false),
    REFUND("Refund", false),
    TRANSFER("Move money", false),
    SAVING("Money saved", false),
    DONATION("Donation", true),
    DEBT_PAYMENT("Debt paid", true),
    DEBT_RECEIVED("Debt received", false),
    ADJUSTMENT("Fix balance", false);

    companion object {
        fun fromString(value: String): TransactionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPENSE
        }
    }
}
