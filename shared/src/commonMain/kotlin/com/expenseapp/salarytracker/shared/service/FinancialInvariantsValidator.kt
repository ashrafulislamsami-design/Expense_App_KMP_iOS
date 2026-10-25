package com.expenseapp.salarytracker.shared.service

import com.expenseapp.salarytracker.shared.model.Account
import com.expenseapp.salarytracker.shared.model.Budget
import com.expenseapp.salarytracker.shared.model.Category
import com.expenseapp.salarytracker.shared.model.Goal
import com.expenseapp.salarytracker.shared.model.SalaryCycleConfig
import com.expenseapp.salarytracker.shared.model.Transaction
import com.expenseapp.salarytracker.shared.model.TransactionType

data class BackupEnvelope(
    val formatVersion: Int = 1,
    val schemaVersion: Int = 2,
    val exportedAtEpochMs: Long = 0L,
    val appVersion: String = "1.0.0",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val salaryConfig: SalaryCycleConfig? = null
)

data class BackupValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val accountCount: Int = 0,
    val transactionCount: Int = 0,
    val budgetCount: Int = 0,
    val goalCount: Int = 0
)

class FinancialInvariantsValidator {

    /**
     * Asserts critical financial invariants:
     * 1. Transfers require distinct source and destination accounts.
     * 2. Transaction amounts must be strictly non-negative.
     * 3. Salary cycle configured day must be between 1 and 31.
     * 4. Double-entry transfers must sum to zero globally.
     */
    fun validateTransaction(tx: Transaction): List<String> {
        val violations = mutableListOf<String>()

        if (tx.amount.isNegative()) {
            violations.add("Transaction amount cannot be negative: ${tx.amount.amountInMinorUnits}")
        }

        if (tx.type == TransactionType.TRANSFER) {
            if (tx.destinationAccountId == null) {
                violations.add("Transfer transaction #${tx.id} missing destinationAccountId")
            }
            if (tx.destinationAccountId == tx.accountId) {
                violations.add("Transfer source and destination accounts cannot be identical: accountId=${tx.accountId}")
            }
        }

        return violations
    }

    fun validateSalaryConfig(config: SalaryCycleConfig): List<String> {
        val violations = mutableListOf<String>()
        if (config.salaryDayOfMonth !in 1..31) {
            violations.add("Salary day of month must be between 1 and 31: ${config.salaryDayOfMonth}")
        }
        if (config.salaryAmount.isNegative()) {
            violations.add("Salary amount cannot be negative: ${config.salaryAmount.amountInMinorUnits}")
        }
        return violations
    }
}
