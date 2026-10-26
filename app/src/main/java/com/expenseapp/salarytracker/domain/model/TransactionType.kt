package com.expenseapp.salarytracker.domain.model

enum class TransactionType(val displayName: String, val isExpenseLike: Boolean) {
    EXPENSE("Expense", true),
    INCOME("Income", false),
    REFUND("Refund", false),
    TRANSFER("Transfer", false),
    SAVING("Savings", false),
    DONATION("Donation", true),
    DEBT_PAYMENT("Debt Repayment", true),
    DEBT_RECEIVED("Debt Inflow", false),
    ADJUSTMENT("Balance Adjustment", false);

    companion object {
        fun fromString(value: String): TransactionType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPENSE
        }
    }
}
