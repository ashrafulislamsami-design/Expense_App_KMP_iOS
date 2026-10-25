package com.expenseapp.salarytracker.shared.service

import com.expenseapp.salarytracker.shared.model.Account
import com.expenseapp.salarytracker.shared.model.Budget
import com.expenseapp.salarytracker.shared.model.Category
import com.expenseapp.salarytracker.shared.model.FinancialDate
import com.expenseapp.salarytracker.shared.model.MinorUnits
import com.expenseapp.salarytracker.shared.model.Transaction
import com.expenseapp.salarytracker.shared.model.TransactionType

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
    val variance: MinorUnits,
    val percentageUsed: Double,
    val isOverBudget: Boolean,
    val isWarning: Boolean
)

class FinancialCalculationEngine {

    /**
     * Calculates financial summary across a collection of transactions.
     * Enforces double-entry transfer invariants, refund offsets, and keeps debt isolated.
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
                TransactionType.INCOME -> income += tx.amount
                TransactionType.EXPENSE -> rawExpense += tx.amount
                TransactionType.REFUND -> refunds += tx.amount
                TransactionType.TRANSFER -> { /* Transfers do NOT affect global income or global expense */ }
                TransactionType.SAVING -> savings += tx.amount
                TransactionType.DONATION -> {
                    donations += tx.amount
                    rawExpense += tx.amount
                }
                TransactionType.DEBT_PAYMENT -> debtPayment += tx.amount
                TransactionType.DEBT_RECEIVED -> debtReceived += tx.amount
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
        val totalOperationalInflows = income + debtReceived + adjustmentInflow
        val totalOperationalOutflows = rawExpense - refunds + debtPayment + savings + adjustmentOutflow
        val netCashFlow = totalOperationalInflows - totalOperationalOutflows

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
     * Calculates the real-time balance of a specific account.
     */
    fun calculateAccountBalance(account: Account, allTransactions: Collection<Transaction>): MinorUnits {
        var balance = account.initialBalance

        for (tx in allTransactions) {
            if (tx.accountId == account.id) {
                when (tx.type) {
                    TransactionType.INCOME,
                    TransactionType.REFUND,
                    TransactionType.DEBT_RECEIVED -> balance += tx.amount
                    TransactionType.EXPENSE,
                    TransactionType.SAVING,
                    TransactionType.DONATION,
                    TransactionType.DEBT_PAYMENT -> balance -= tx.amount
                    TransactionType.TRANSFER -> balance -= tx.amount // Outflow from source
                    TransactionType.ADJUSTMENT -> balance += tx.amount
                }
            } else if (tx.destinationAccountId == account.id && tx.type == TransactionType.TRANSFER) {
                balance += tx.amount // Inflow to destination
            }
        }
        return balance
    }

    /**
     * Dynamic safe daily spend pacing.
     */
    fun calculateSafeToSpend(
        window: DateWindow,
        currentDate: FinancialDate,
        overallBudget: Budget?,
        salaryIncome: MinorUnits,
        periodTransactions: Collection<Transaction>
    ): SafeToSpendEstimate {
        val totalAvailableBudget = overallBudget?.limitAmount ?: salaryIncome
        val summary = calculateSummary(periodTransactions)
        val currentSpend = summary.netExpense

        val remainingBudget = if (totalAvailableBudget > currentSpend) {
            totalAvailableBudget - currentSpend
        } else {
            MinorUnits.ZERO
        }

        val remainingDays = window.remainingDays(currentDate).coerceAtLeast(1)
        val safePerDay = if (remainingBudget.isPositive()) {
            remainingBudget / remainingDays
        } else {
            MinorUnits.ZERO
        }

        val explanation = when {
            remainingBudget.isZero() && currentSpend >= totalAvailableBudget ->
                "Budget depleted for this cycle."
            currentDate.isAfter(window.endDate) ->
                "Cycle completed."
            else ->
                "${safePerDay.formatCompact()} daily allowance for remaining $remainingDays day(s)."
        }

        return SafeToSpendEstimate(
            safeAmountPerDay = safePerDay,
            remainingBudget = remainingBudget,
            remainingDays = remainingDays,
            explanation = explanation
        )
    }

    /**
     * Computes spending per category, factoring in category-linked refunds.
     */
    fun calculateCategoryBreakdown(transactions: Collection<Transaction>): Map<Long, MinorUnits> {
        val expenseMap = mutableMapOf<Long, MinorUnits>()
        val refundMap = mutableMapOf<Long, MinorUnits>()

        for (tx in transactions) {
            val catId = tx.categoryId ?: continue
            when (tx.type) {
                TransactionType.EXPENSE, TransactionType.DONATION -> {
                    val current = expenseMap.getOrElse(catId) { MinorUnits.ZERO }
                    expenseMap[catId] = current + tx.amount
                }
                TransactionType.REFUND -> {
                    val current = refundMap.getOrElse(catId) { MinorUnits.ZERO }
                    refundMap[catId] = current + tx.amount
                }
                else -> { /* Ignore non-expense types */ }
            }
        }

        val resultMap = mutableMapOf<Long, MinorUnits>()
        for ((catId, expense) in expenseMap) {
            val refund = refundMap.getOrElse(catId) { MinorUnits.ZERO }
            val net = if (expense > refund) expense - refund else MinorUnits.ZERO
            resultMap[catId] = net
        }
        return resultMap
    }

    /**
     * Calculates status for all active budgets.
     */
    fun calculateAllBudgetsStatus(
        budgets: List<Budget>,
        periodTransactions: Collection<Transaction>,
        categoriesMap: Map<Long, Category>
    ): List<BudgetStatus> {
        val categorySpend = calculateCategoryBreakdown(periodTransactions)
        val overallSummary = calculateSummary(periodTransactions)

        return budgets.map { budget ->
            val actual = if (budget.categoryId == null) {
                overallSummary.netExpense
            } else {
                categorySpend.getOrElse(budget.categoryId) { MinorUnits.ZERO }
            }

            val variance = budget.limitAmount - actual
            val isOver = actual > budget.limitAmount
            val remaining = if (variance.isPositive()) variance else MinorUnits.ZERO

            val pct = if (budget.limitAmount.isPositive()) {
                (actual.amountInMinorUnits.toDouble() / budget.limitAmount.amountInMinorUnits.toDouble()) * 100.0
            } else 0.0

            val isWarning = pct >= budget.warningThresholdPercentage && !isOver

            BudgetStatus(
                budget = budget,
                category = budget.categoryId?.let { categoriesMap[it] },
                plannedAmount = budget.limitAmount,
                actualAmount = actual,
                remainingAmount = remaining,
                variance = variance,
                percentageUsed = pct,
                isOverBudget = isOver,
                isWarning = isWarning
            )
        }
    }
}
