package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeHistory
import com.expenseapp.salarytracker.domain.model.report.AccountReportItem
import com.expenseapp.salarytracker.domain.model.report.BudgetReportItem
import com.expenseapp.salarytracker.domain.model.report.CategoryReportItem
import com.expenseapp.salarytracker.domain.model.report.DailyReportItem
import com.expenseapp.salarytracker.domain.model.report.FinancialReport
import com.expenseapp.salarytracker.domain.model.report.GoalReportItem
import com.expenseapp.salarytracker.domain.model.report.ReportDateRange
import com.expenseapp.salarytracker.domain.model.report.ReportPeriodType
import com.expenseapp.salarytracker.domain.model.report.ReportTransactionItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class ReportGenerationService(
    private val calculationEngine: FinancialCalculationEngine = FinancialCalculationEngine(),
    private val salaryCycleCalculator: SalaryCycleCalculator = SalaryCycleCalculator(),
    private val goalCalculationEngine: GoalCalculationEngine = GoalCalculationEngine()
) {

    /**
     * Resolves a [ReportPeriodType] into an exact [ReportDateRange] based on reference date and salary configuration.
     */
    fun resolveDateRange(
        periodType: ReportPeriodType,
        referenceDate: LocalDate = LocalDate.now(),
        salaryConfig: SalaryCycleConfig,
        customStart: LocalDate? = null,
        customEnd: LocalDate? = null
    ): ReportDateRange {
        return when (periodType) {
            ReportPeriodType.TODAY -> {
                ReportDateRange(referenceDate, referenceDate, periodType, "Today (${referenceDate})")
            }
            ReportPeriodType.YESTERDAY -> {
                val y = referenceDate.minusDays(1)
                ReportDateRange(y, y, periodType, "Yesterday (${y})")
            }
            ReportPeriodType.THIS_WEEK -> {
                val start = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val end = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                ReportDateRange(start, end, periodType, "This Week ($start to $end)")
            }
            ReportPeriodType.LAST_WEEK -> {
                val thisWeekMon = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val start = thisWeekMon.minusWeeks(1)
                val end = start.plusDays(6)
                ReportDateRange(start, end, periodType, "Last Week ($start to $end)")
            }
            ReportPeriodType.THIS_MONTH -> {
                val start = referenceDate.withDayOfMonth(1)
                val end = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth())
                val monthName = referenceDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
                ReportDateRange(start, end, periodType, "$monthName ${referenceDate.year}")
            }
            ReportPeriodType.LAST_MONTH -> {
                val prevMonth = referenceDate.minusMonths(1)
                val start = prevMonth.withDayOfMonth(1)
                val end = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
                val monthName = prevMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
                ReportDateRange(start, end, periodType, "$monthName ${prevMonth.year}")
            }
            ReportPeriodType.CURRENT_SALARY_CYCLE -> {
                val window = salaryCycleCalculator.calculateWindow(
                    referenceDate,
                    salaryConfig.copy(reportingMode = com.expenseapp.salarytracker.domain.model.ReportingCycleMode.SALARY_CYCLE)
                )
                ReportDateRange(window.startDate, window.endDate, periodType, "Salary Cycle (${window.startDate} to ${window.endDate})")
            }
            ReportPeriodType.PREVIOUS_SALARY_CYCLE -> {
                val currentWindow = salaryCycleCalculator.calculateWindow(
                    referenceDate,
                    salaryConfig.copy(reportingMode = com.expenseapp.salarytracker.domain.model.ReportingCycleMode.SALARY_CYCLE)
                )
                val prevWindow = salaryCycleCalculator.calculateWindow(
                    currentWindow.startDate.minusDays(1),
                    salaryConfig.copy(reportingMode = com.expenseapp.salarytracker.domain.model.ReportingCycleMode.SALARY_CYCLE)
                )
                ReportDateRange(prevWindow.startDate, prevWindow.endDate, periodType, "Salary Cycle (${prevWindow.startDate} to ${prevWindow.endDate})")
            }
            ReportPeriodType.CUSTOM -> {
                val start = customStart ?: referenceDate.withDayOfMonth(1)
                val end = customEnd ?: referenceDate
                require(!start.isAfter(end)) { "Start date ($start) cannot be after end date ($end)" }
                ReportDateRange(start, end, periodType, "Custom ($start to $end)")
            }
        }
    }

    /**
     * Generates a comprehensive, internally consistent [FinancialReport].
     */
    fun generateReport(
        dateRange: ReportDateRange,
        allTransactions: List<Transaction>,
        accounts: List<Account>,
        categories: List<Category>,
        budgets: List<Budget>,
        goals: List<Goal>,
        treeHistory: List<TreeHistory>,
        currencyCode: String = "BDT",
        currencySymbol: String = "\u09F3"
    ): FinancialReport {
        val startDate = dateRange.startDate
        val endDate = dateRange.endDate

        // 1. Separate transactions before period (for opening balance) and within period
        val priorTransactions = allTransactions.filter { it.date.isBefore(startDate) }
        val periodTransactions = allTransactions.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }

        // 2. Global summary for the period
        val summary = calculationEngine.calculateSummary(periodTransactions)

        // 3. Opening and Closing Balances across all accounts
        var totalOpeningBalance = MinorUnits.ZERO
        var totalClosingBalance = MinorUnits.ZERO

        val accountItems = mutableListOf<AccountReportItem>()
        val categoriesMap = categories.associateBy { it.id }
        val accountsMap = accounts.associateBy { it.id }

        for (acc in accounts) {
            val accPriorTx = priorTransactions.filter { it.accountId == acc.id || it.destinationAccountId == acc.id }
            val accPeriodTx = periodTransactions.filter { it.accountId == acc.id || it.destinationAccountId == acc.id }

            val openingBal = calculationEngine.calculateAccountBalance(acc, accPriorTx)
            val closingBal = calculationEngine.calculateAccountBalance(acc, accPriorTx + accPeriodTx)

            totalOpeningBalance += openingBal
            totalClosingBalance += closingBal

            var inflows = MinorUnits.ZERO
            var outflows = MinorUnits.ZERO
            var transfersIn = MinorUnits.ZERO
            var transfersOut = MinorUnits.ZERO

            for (tx in accPeriodTx) {
                if (tx.type == TransactionType.TRANSFER) {
                    if (tx.destinationAccountId == acc.id) {
                        transfersIn += tx.amount
                    }
                    if (tx.accountId == acc.id) {
                        transfersOut += tx.amount
                    }
                } else if (tx.accountId == acc.id) {
                    when (tx.type) {
                        TransactionType.INCOME,
                        TransactionType.REFUND,
                        TransactionType.DEBT_RECEIVED -> inflows += tx.amount
                        TransactionType.EXPENSE,
                        TransactionType.DONATION,
                        TransactionType.SAVING,
                        TransactionType.DEBT_PAYMENT -> outflows += tx.amount
                        TransactionType.ADJUSTMENT -> {
                            if (tx.amount.isPositive()) inflows += tx.amount else outflows += -tx.amount
                        }
                        TransactionType.TRANSFER -> Unit
                    }
                }
            }

            accountItems.add(
                AccountReportItem(
                    accountId = acc.id,
                    accountName = acc.name,
                    accountType = acc.type.displayName,
                    openingBalance = openingBal,
                    totalInflows = inflows,
                    totalOutflows = outflows,
                    transfersIn = transfersIn,
                    transfersOut = transfersOut,
                    closingBalance = closingBal
                )
            )
        }

        // 4. Category breakdown
        val rawCatBreakdown = calculationEngine.calculateCategoryBreakdown(periodTransactions)
        val netTotalSpending = summary.netExpense
        val categoryItems = rawCatBreakdown.mapNotNull { (catId, amount) ->
            val cat = categoriesMap[catId] ?: return@mapNotNull null
            val pct = if (netTotalSpending.isPositive()) {
                (amount.amountInMinorUnits.toDouble() / netTotalSpending.amountInMinorUnits.toDouble()) * 100.0
            } else 0.0
            val count = periodTransactions.count { it.categoryId == catId && (it.type == TransactionType.EXPENSE || it.type == TransactionType.DONATION) }
            CategoryReportItem(
                categoryId = cat.id,
                categoryName = cat.name,
                colorHex = cat.colorHex,
                amount = amount,
                percentageOfTotal = pct,
                transactionCount = count
            )
        }.sortedByDescending { it.amount }

        // 5. Daily Breakdown
        val dailyMap = periodTransactions.groupBy { it.date }
        val dailyItems = mutableListOf<DailyReportItem>()
        var curDate = startDate
        while (!curDate.isAfter(endDate)) {
            val dayTx = dailyMap[curDate] ?: emptyList()
            val daySum = calculationEngine.calculateSummary(dayTx)
            dailyItems.add(
                DailyReportItem(
                    date = curDate,
                    expenseAmount = daySum.netExpense,
                    incomeAmount = daySum.totalIncome,
                    netCashFlow = daySum.netCashFlow,
                    transactionCount = dayTx.size
                )
            )
            curDate = curDate.plusDays(1)
        }

        // 6. Budgets
        val allBudgetsStatus = calculationEngine.calculateAllBudgetsStatus(budgets, periodTransactions, categoriesMap)
        var overallBudgetItem: BudgetReportItem? = null
        val budgetItems = mutableListOf<BudgetReportItem>()

        for (bs in allBudgetsStatus) {
            val statusText = when {
                bs.isOverBudget -> "Over Budget"
                bs.isWarning -> "Warning (${bs.percentageUsed.toInt()}%)"
                else -> "On Track"
            }
            val item = BudgetReportItem(
                budgetId = bs.budget.id,
                name = bs.category?.name ?: "Overall Budget",
                categoryName = bs.category?.name,
                plannedAmount = bs.plannedAmount,
                actualAmount = bs.actualAmount,
                remainingAmount = bs.remainingAmount,
                variance = bs.variance,
                percentageUsed = bs.percentageUsed,
                statusText = statusText,
                isOverBudget = bs.isOverBudget,
                isWarning = bs.isWarning
            )
            if (bs.budget.categoryId == null) {
                overallBudgetItem = item
            } else {
                budgetItems.add(item)
            }
        }

        // 7. Goals
        val goalItems = goals.map { g ->
            val prog = goalCalculationEngine.calculateGoalProgress(g, allTransactions)
            GoalReportItem(
                goalId = g.id,
                name = g.name,
                type = g.type,
                targetAmount = g.targetAmount,
                currentProgress = prog.currentAmount,
                percentageReached = prog.progressPercentage,
                status = g.status,
                reachedMilestones = g.reachedMilestones,
                startDate = g.startDate,
                endDate = g.endDate
            )
        }

        // 8. Tree of Hope
        val activeGoalsProgress = goals.filter { it.status == com.expenseapp.salarytracker.domain.model.GoalStatus.ACTIVE }
            .map { goalCalculationEngine.calculateGoalProgress(it, allTransactions) }
        val overallBudgetStatus = allBudgetsStatus.firstOrNull { it.budget.categoryId == null }
        val treeOfHopeStatus = goalCalculationEngine.calculateTreeOfHopeState(
            activeGoalsWithProgress = activeGoalsProgress,
            overallBudgetStatus = overallBudgetStatus
        )

        // 9. Savings Rate
        val savingsRate = if (summary.totalIncome.isPositive()) {
            (summary.totalSavings.amountInMinorUnits.toDouble() / summary.totalIncome.amountInMinorUnits.toDouble()) * 100.0
        } else 0.0

        // 10. Itemized Transactions
        val reportTransactions = periodTransactions.sortedWith(
            compareByDescending<Transaction> { it.date }
                .thenByDescending { it.time }
                .thenByDescending { it.id }
        ).map { tx ->
            ReportTransactionItem(
                id = tx.id,
                date = tx.date,
                time = tx.time,
                type = tx.type,
                amount = tx.amount,
                currency = currencyCode,
                accountId = tx.accountId,
                accountName = accountsMap[tx.accountId]?.name ?: "Account #${tx.accountId}",
                destinationAccountId = tx.destinationAccountId,
                destinationAccountName = tx.destinationAccountId?.let { accountsMap[it]?.name ?: "Account #$it" },
                categoryId = tx.categoryId,
                categoryName = tx.categoryId?.let { categoriesMap[it]?.name ?: "Category #$it" },
                description = tx.note.ifBlank { tx.type.displayName },
                notes = tx.note,
                recurringRuleId = tx.recurringRuleId,
                customFields = tx.customFieldValues,
                createdAtEpochMs = tx.createdAtEpochMs,
                updatedAtEpochMs = tx.updatedAtEpochMs
            )
        }

        return FinancialReport(
            periodLabel = dateRange.label,
            startDate = startDate,
            endDate = endDate,
            generatedAtEpochMs = System.currentTimeMillis(),
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            openingBalance = totalOpeningBalance,
            closingBalance = totalClosingBalance,
            totalIncome = summary.totalIncome,
            totalExpenses = summary.totalExpense,
            totalRefunds = summary.totalRefunds,
            netExpenses = summary.netExpense,
            netSavings = summary.totalSavings,
            savingsRatePercent = savingsRate,
            transfersTotal = summary.transfersTotal,
            donationsTotal = summary.totalDonations,
            debtPaidTotal = summary.totalDebtPayment,
            debtReceivedTotal = summary.totalDebtReceived,
            netCashFlow = summary.netCashFlow,
            transactionCount = periodTransactions.size,
            categoryBreakdown = categoryItems,
            dailyBreakdown = dailyItems,
            accountBreakdown = accountItems,
            budgetBreakdown = budgetItems,
            overallBudget = overallBudgetItem,
            goalBreakdown = goalItems,
            treeState = treeOfHopeStatus.state,
            treeLevel = treeOfHopeStatus.state.level,
            treeExplanation = treeOfHopeStatus.explanation,
            treeActionAdvice = treeOfHopeStatus.positiveActionTip,
            treeHistory = treeHistory,
            transactions = reportTransactions
        )
    }

    // Helper alias
    private val com.expenseapp.salarytracker.domain.service.FinancialSummary.transfersTotal: MinorUnits
        get() = MinorUnits.ZERO // transfers invariant: 0 impact on summary totals
}
