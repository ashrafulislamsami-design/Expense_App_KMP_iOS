package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import java.time.LocalDate

data class FinancialSummary(
    val totalIncome: MinorUnits = MinorUnits.ZERO,
    val totalExpense: MinorUnits = MinorUnits.ZERO,
    val totalRefunds: MinorUnits = MinorUnits.ZERO,
    val netExpense: MinorUnits = MinorUnits.ZERO,
    val totalSavings: MinorUnits = MinorUnits.ZERO,
    val totalDonations: MinorUnits = MinorUnits.ZERO,
    val totalDebtPayment: MinorUnits = MinorUnits.ZERO,
    val totalDebtReceived: MinorUnits = MinorUnits.ZERO,
    val netCashFlow: MinorUnits = MinorUnits.ZERO,
    val transactionCount: Int = 0
) {
    // Backward-compatible alias for net cash flow
    val netChange: MinorUnits get() = netCashFlow
}

data class SafeToSpendEstimate(
    val safeAmountPerDay: MinorUnits,
    val remainingBudget: MinorUnits,
    val remainingDays: Long,
    val explanation: String
)

data class BudgetStatus(
    val budget: Budget,
    val category: Category? = null,
    val plannedAmount: MinorUnits,
    val actualAmount: MinorUnits,
    val remainingAmount: MinorUnits,
    val variance: MinorUnits, // plannedAmount - actualAmount (> 0 is under budget, < 0 is over budget)
    val percentageUsed: Double, // 0.0 to 100.0+
    val isOverBudget: Boolean,
    val isWarning: Boolean
)

class FinancialCalculationEngine {

    /**
     * Calculates financial summary across a collection of transactions.
     * Enforces double-entry transfer invariants, refund offsets, and keeps debt isolated
     * from ordinary operational spending/income.
     */
    fun calculateSummary(transactions: Collection<Transaction>): FinancialSummary {
        var income = MinorUnits.ZERO
        var rawExpense = MinorUnits.ZERO
        var refunds = MinorUnits.ZERO
        var savings = MinorUnits.ZERO
        var donations = MinorUnits.ZERO
        var debtPayment = MinorUnits.ZERO
        var debtReceived = MinorUnits.ZERO
        var adjustmentInflow = MinorUnits.ZERO
        var adjustmentOutflow = MinorUnits.ZERO

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    income += tx.amount
                }
                TransactionType.EXPENSE -> {
                    rawExpense += tx.amount
                }
                TransactionType.REFUND -> {
                    // Refunds offset expenses, not gross income
                    refunds += tx.amount
                }
                TransactionType.TRANSFER -> {
                    // STRICT INVARIANT: Transfers do NOT affect global income or global expense
                }
                TransactionType.SAVING -> {
                    savings += tx.amount
                }
                TransactionType.DONATION -> {
                    donations += tx.amount
                    rawExpense += tx.amount
                }
                TransactionType.DEBT_PAYMENT -> {
                    // Financing outflow: tracked explicitly without distorting ordinary consumption expenses
                    debtPayment += tx.amount
                }
                TransactionType.DEBT_RECEIVED -> {
                    // Financing inflow: tracked explicitly without distorting ordinary earned income
                    debtReceived += tx.amount
                }
                TransactionType.ADJUSTMENT -> {
                    if (tx.amount.isPositive()) {
                        adjustmentInflow += tx.amount
                    } else {
                        adjustmentOutflow += -tx.amount
                    }
                }
            }
        }

        val netExpense = if (rawExpense > refunds) rawExpense - refunds else MinorUnits.ZERO
        // Net Cash Flow = Total Inflows - Total Outflows
        val totalInflows = income + debtReceived + refunds + adjustmentInflow
        val totalOutflows = rawExpense + debtPayment + savings + adjustmentOutflow
        val netCashFlow = totalInflows - totalOutflows

        return FinancialSummary(
            totalIncome = income,
            totalExpense = rawExpense,
            totalRefunds = refunds,
            netExpense = netExpense,
            totalSavings = savings,
            totalDonations = donations,
            totalDebtPayment = debtPayment,
            totalDebtReceived = debtReceived,
            netCashFlow = netCashFlow,
            transactionCount = transactions.size
        )
    }

    /**
     * Calculates the balance of a single account given its initial balance and all transactions.
     */
    fun calculateAccountBalance(account: Account, transactions: Collection<Transaction>): MinorUnits {
        var balance = account.initialBalance

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.TRANSFER -> {
                    if (tx.accountId == account.id) {
                        // Outflow from source account
                        balance -= tx.amount
                    }
                    if (tx.destinationAccountId == account.id) {
                        // Inflow to destination account
                        balance += tx.amount
                    }
                }
                else -> {
                    if (tx.accountId == account.id) {
                        when (tx.type) {
                            TransactionType.INCOME,
                            TransactionType.REFUND,
                            TransactionType.DEBT_RECEIVED -> {
                                balance += tx.amount
                            }
                            TransactionType.EXPENSE,
                            TransactionType.DONATION,
                            TransactionType.DEBT_PAYMENT,
                            TransactionType.SAVING -> {
                                balance -= tx.amount
                            }
                            TransactionType.ADJUSTMENT -> {
                                balance += tx.amount
                            }
                            TransactionType.TRANSFER -> Unit
                        }
                    }
                }
            }
        }

        return balance
    }

    /**
     * Calculates status of a specific Budget against a set of transactions.
     * Respects category filtering, overall spending exclusions, and refund credits.
     */
    fun calculateBudgetStatus(
        budget: Budget,
        transactions: Collection<Transaction>,
        category: Category? = null
    ): BudgetStatus {
        val planned = budget.limitAmount
        var rawSpent = MinorUnits.ZERO
        var refunds = MinorUnits.ZERO

        for (tx in transactions) {
            val matchesCategory = if (budget.categoryId == null) {
                true // overall budget
            } else {
                tx.categoryId == budget.categoryId
            }

            if (!matchesCategory) continue

            when (tx.type) {
                TransactionType.EXPENSE,
                TransactionType.DONATION -> {
                    rawSpent += tx.amount
                }
                TransactionType.REFUND -> {
                    refunds += tx.amount
                }
                // TRANSFERS, SAVINGS, DEBTS, and INCOME are NEVER counted towards consumption budget
                TransactionType.TRANSFER,
                TransactionType.SAVING,
                TransactionType.DEBT_PAYMENT,
                TransactionType.DEBT_RECEIVED,
                TransactionType.INCOME,
                TransactionType.ADJUSTMENT -> Unit
            }
        }

        val actualSpent = if (rawSpent > refunds) rawSpent - refunds else MinorUnits.ZERO
        val remaining = if (planned > actualSpent) planned - actualSpent else MinorUnits.ZERO
        val variance = planned - actualSpent // positive = under, negative = over

        val percentageUsed = if (planned.amountInMinorUnits > 0L) {
            (actualSpent.amountInMinorUnits.toDouble() / planned.amountInMinorUnits.toDouble()) * 100.0
        } else {
            if (actualSpent.isPositive()) 100.0 else 0.0
        }

        val isOverBudget = actualSpent > planned
        val isWarning = !isOverBudget && percentageUsed >= budget.warningThresholdPercentage

        return BudgetStatus(
            budget = budget,
            category = category,
            plannedAmount = planned,
            actualAmount = actualSpent,
            remainingAmount = remaining,
            variance = variance,
            percentageUsed = percentageUsed,
            isOverBudget = isOverBudget,
            isWarning = isWarning
        )
    }

    /**
     * Calculates status for a list of budgets.
     */
    fun calculateAllBudgetsStatus(
        budgets: List<Budget>,
        transactions: Collection<Transaction>,
        categoriesMap: Map<Long, Category>
    ): List<BudgetStatus> {
        return budgets.map { budget ->
            val category = budget.categoryId?.let { categoriesMap[it] }
            calculateBudgetStatus(budget, transactions, category)
        }
    }

    /**
     * Calculates the "Safe to spend today" figure.
     * Takes into account remaining budget/income, days left in cycle, and active savings.
     */
    fun calculateSafeToSpend(
        window: DateWindow,
        currentDate: LocalDate,
        overallBudget: Budget?,
        salaryIncome: MinorUnits,
        periodTransactions: Collection<Transaction>
    ): SafeToSpendEstimate {
        val remainingDays = window.remainingDays(currentDate).coerceAtLeast(1L)
        val summary = calculateSummary(periodTransactions)

        val totalAvailable = if (overallBudget != null && overallBudget.limitAmount > MinorUnits.ZERO) {
            overallBudget.limitAmount
        } else {
            salaryIncome
        }

        val remainingFunds = if (totalAvailable > summary.netExpense) {
            totalAvailable - summary.netExpense
        } else {
            MinorUnits.ZERO
        }

        val safePerDayAmount = MinorUnits(remainingFunds.amountInMinorUnits / remainingDays)

        val explanation = if (overallBudget != null) {
            "Based on your budget of ${overallBudget.limitAmount.format()}, ${remainingFunds.format()} remains across $remainingDays day(s)."
        } else {
            "Based on salary of ${salaryIncome.format()}, ${remainingFunds.format()} remains across $remainingDays day(s)."
        }

        return SafeToSpendEstimate(
            safeAmountPerDay = safePerDayAmount,
            remainingBudget = remainingFunds,
            remainingDays = remainingDays,
            explanation = explanation
        )
    }

    /**
     * Groups expenses by category ID, subtracting any refunds belonging to each category.
     * Transfers, savings, and debts are strictly excluded.
     */
    fun calculateCategoryBreakdown(transactions: Collection<Transaction>): Map<Long, MinorUnits> {
        val categoryTotals = mutableMapOf<Long, Long>()

        for (tx in transactions) {
            val catId = tx.categoryId ?: continue
            when (tx.type) {
                TransactionType.EXPENSE,
                TransactionType.DONATION -> {
                    categoryTotals[catId] = (categoryTotals[catId] ?: 0L) + tx.amount.amountInMinorUnits
                }
                TransactionType.REFUND -> {
                    val current = categoryTotals[catId] ?: 0L
                    categoryTotals[catId] = maxOf(0L, current - tx.amount.amountInMinorUnits)
                }
                else -> Unit
            }
        }

        return categoryTotals.mapValues { MinorUnits(it.value) }
    }
}
