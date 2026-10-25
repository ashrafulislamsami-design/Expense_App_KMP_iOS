package com.expenseapp.salarytracker.shared.model

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

data class Transaction(
    val id: Long = 0,
    val amount: MinorUnits,
    val type: TransactionType,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val date: FinancialDate,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val recurringRuleId: Long? = null,
    val originalTransactionId: Long? = null,
    val customFieldValues: Map<String, String> = emptyMap(),
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)
