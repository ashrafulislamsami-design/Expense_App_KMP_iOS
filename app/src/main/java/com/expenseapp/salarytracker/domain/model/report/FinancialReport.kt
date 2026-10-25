package com.expenseapp.salarytracker.domain.model.report

import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeHistory
import com.expenseapp.salarytracker.domain.model.TreeState
import java.time.LocalDate
import java.time.LocalTime

data class CategoryReportItem(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val amount: MinorUnits,
    val percentageOfTotal: Double,
    val transactionCount: Int
)

data class DailyReportItem(
    val date: LocalDate,
    val expenseAmount: MinorUnits,
    val incomeAmount: MinorUnits,
    val netCashFlow: MinorUnits,
    val transactionCount: Int
)

data class AccountReportItem(
    val accountId: Long,
    val accountName: String,
    val accountType: String,
    val openingBalance: MinorUnits,
    val totalInflows: MinorUnits,
    val totalOutflows: MinorUnits,
    val transfersIn: MinorUnits,
    val transfersOut: MinorUnits,
    val closingBalance: MinorUnits
)

data class BudgetReportItem(
    val budgetId: Long,
    val name: String,
    val categoryName: String?,
    val plannedAmount: MinorUnits,
    val actualAmount: MinorUnits,
    val remainingAmount: MinorUnits,
    val variance: MinorUnits,
    val percentageUsed: Double,
    val statusText: String,
    val isOverBudget: Boolean,
    val isWarning: Boolean
)

data class GoalReportItem(
    val goalId: Long,
    val name: String,
    val type: GoalType,
    val targetAmount: MinorUnits,
    val currentProgress: MinorUnits,
    val percentageReached: Double,
    val status: GoalStatus,
    val reachedMilestones: List<Int>,
    val startDate: LocalDate,
    val endDate: LocalDate?
)

data class ReportTransactionItem(
    val id: Long,
    val date: LocalDate,
    val time: LocalTime,
    val type: TransactionType,
    val amount: MinorUnits,
    val currency: String,
    val accountId: Long,
    val accountName: String,
    val destinationAccountId: Long?,
    val destinationAccountName: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val description: String,
    val notes: String,
    val recurringRuleId: Long?,
    val customFields: Map<String, String>,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

data class FinancialReport(
    val periodLabel: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val generatedAtEpochMs: Long,
    val currencyCode: String,
    val currencySymbol: String,

    // Summary KPIs
    val openingBalance: MinorUnits,
    val closingBalance: MinorUnits,
    val totalIncome: MinorUnits,
    val totalExpenses: MinorUnits, // Gross expense + donations
    val totalRefunds: MinorUnits,
    val netExpenses: MinorUnits,   // Total expenses - refunds
    val netSavings: MinorUnits,    // Allocations to savings
    val savingsRatePercent: Double,
    val transfersTotal: MinorUnits,
    val donationsTotal: MinorUnits,
    val debtPaidTotal: MinorUnits,
    val debtReceivedTotal: MinorUnits,
    val netCashFlow: MinorUnits,
    val transactionCount: Int,

    // Detailed Breakdowns
    val categoryBreakdown: List<CategoryReportItem>,
    val dailyBreakdown: List<DailyReportItem>,
    val accountBreakdown: List<AccountReportItem>,
    val budgetBreakdown: List<BudgetReportItem>,
    val overallBudget: BudgetReportItem?,
    val goalBreakdown: List<GoalReportItem>,

    // Tree of Hope Status
    val treeState: TreeState,
    val treeLevel: Int,
    val treeExplanation: String,
    val treeActionAdvice: String,
    val treeHistory: List<TreeHistory>,

    // Itemized Transactions
    val transactions: List<ReportTransactionItem>
)
