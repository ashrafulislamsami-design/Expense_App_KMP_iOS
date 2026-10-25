package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.ReportingCycleMode
import com.expenseapp.salarytracker.domain.model.SalaryCycleConfig
import com.expenseapp.salarytracker.domain.model.SalaryFrequency
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.report.ReportDateRange
import com.expenseapp.salarytracker.domain.model.report.ReportPeriodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReportGenerationServiceTest {

    private val reportService = ReportGenerationService()

    private val salaryConfig = SalaryCycleConfig(
        salaryDayOfMonth = 10,
        anchorDate = LocalDate.of(2026, 9, 10),
        frequency = SalaryFrequency.MONTHLY,
        reportingMode = ReportingCycleMode.SALARY_CYCLE
    )

    private val checkingAccount = Account(
        id = 1L,
        name = "Bank Checking",
        type = AccountType.BANK,
        initialBalance = MinorUnits(1000000L), // 10,000.00
        currency = "BDT"
    )

    private val cashAccount = Account(
        id = 2L,
        name = "Cash Wallet",
        type = AccountType.CASH,
        initialBalance = MinorUnits(200000L), // 2,000.00
        currency = "BDT"
    )

    private val foodCategory = Category(id = 1L, name = "Food", colorHex = "#F59E0B")
    private val transportCategory = Category(id = 2L, name = "Transport", colorHex = "#3B82F6")

    @Test
    fun `resolveDateRange handles salary cycle correctly`() {
        val refDate = LocalDate.of(2026, 9, 15)
        val cycle = reportService.resolveDateRange(ReportPeriodType.CURRENT_SALARY_CYCLE, refDate, salaryConfig)

        assertEquals(LocalDate.of(2026, 9, 10), cycle.startDate)
        assertEquals(LocalDate.of(2026, 10, 9), cycle.endDate)
    }

    @Test
    fun `resolveDateRange handles previous salary cycle correctly`() {
        val refDate = LocalDate.of(2026, 9, 15)
        val prevCycle = reportService.resolveDateRange(ReportPeriodType.PREVIOUS_SALARY_CYCLE, refDate, salaryConfig)

        assertEquals(LocalDate.of(2026, 8, 10), prevCycle.startDate)
        assertEquals(LocalDate.of(2026, 9, 9), prevCycle.endDate)
    }

    @Test
    fun `resolveDateRange handles calendar month correctly`() {
        val refDate = LocalDate.of(2026, 9, 15)
        val monthRange = reportService.resolveDateRange(ReportPeriodType.THIS_MONTH, refDate, salaryConfig)

        assertEquals(LocalDate.of(2026, 9, 1), monthRange.startDate)
        assertEquals(LocalDate.of(2026, 9, 30), monthRange.endDate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `resolveDateRange throws on invalid custom range`() {
        reportService.resolveDateRange(
            periodType = ReportPeriodType.CUSTOM,
            salaryConfig = salaryConfig,
            customStart = LocalDate.of(2026, 9, 20),
            customEnd = LocalDate.of(2026, 9, 10)
        )
    }

    @Test
    fun `generateReport enforces opening balance and closing balance continuity`() {
        // Prior transaction on Aug 25 (before period Sept 1 to Sept 30)
        val priorTx = Transaction(
            id = 1L,
            amount = MinorUnits(500000L), // +5,000 income
            type = TransactionType.INCOME,
            accountId = 1L,
            date = LocalDate.of(2026, 8, 25)
        )

        // Period transaction on Sept 5: +50,000 salary
        val tx1 = Transaction(
            id = 2L,
            amount = MinorUnits(5000000L),
            type = TransactionType.INCOME,
            accountId = 1L,
            date = LocalDate.of(2026, 9, 5)
        )

        // Period transaction on Sept 10: -10,000 food expense
        val tx2 = Transaction(
            id = 3L,
            amount = MinorUnits(1000000L),
            type = TransactionType.EXPENSE,
            accountId = 1L,
            categoryId = 1L,
            date = LocalDate.of(2026, 9, 10)
        )

        val dateRange = ReportDateRange(
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            periodType = ReportPeriodType.THIS_MONTH,
            label = "September 2026"
        )

        val report = reportService.generateReport(
            dateRange = dateRange,
            allTransactions = listOf(priorTx, tx1, tx2),
            accounts = listOf(checkingAccount, cashAccount),
            categories = listOf(foodCategory, transportCategory),
            budgets = emptyList(),
            goals = emptyList(),
            treeHistory = emptyList()
        )

        // Initial accounts sum: 10,000 + 2,000 = 12,000 (1,200,000 minor)
        // Prior income: +5,000 (500,000 minor)
        // Opening balance at Sept 1: 17,000 (1,700,000 minor)
        assertEquals(MinorUnits(1700000L), report.openingBalance)

        // Period net cash flow: +50,000 income - 10,000 expense = +40,000 (4,000,000 minor)
        assertEquals(MinorUnits(4000000L), report.netCashFlow)

        // Closing balance: 17,000 + 40,000 = 57,000 (5,700,000 minor)
        assertEquals(MinorUnits(5700000L), report.closingBalance)
    }

    @Test
    fun `generateReport enforces transfer invariance and refund offsets`() {
        val dateRange = ReportDateRange(
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            periodType = ReportPeriodType.THIS_MONTH,
            label = "September 2026"
        )

        // 1. Transfer between accounts: Checking -> Cash 2,000
        val transferTx = Transaction(
            id = 10L,
            amount = MinorUnits(200000L),
            type = TransactionType.TRANSFER,
            accountId = 1L,
            destinationAccountId = 2L,
            date = LocalDate.of(2026, 9, 2)
        )

        // 2. Gross expense on food: 1,500
        val expenseTx = Transaction(
            id = 11L,
            amount = MinorUnits(150000L),
            type = TransactionType.EXPENSE,
            accountId = 2L,
            categoryId = 1L,
            date = LocalDate.of(2026, 9, 5)
        )

        // 3. Refund for returned item: 300
        val refundTx = Transaction(
            id = 12L,
            amount = MinorUnits(30000L),
            type = TransactionType.REFUND,
            accountId = 2L,
            categoryId = 1L,
            date = LocalDate.of(2026, 9, 6)
        )

        val report = reportService.generateReport(
            dateRange = dateRange,
            allTransactions = listOf(transferTx, expenseTx, refundTx),
            accounts = listOf(checkingAccount, cashAccount),
            categories = listOf(foodCategory, transportCategory),
            budgets = emptyList(),
            goals = emptyList(),
            treeHistory = emptyList()
        )

        // Transfers must NEVER appear in total income or total expenses
        assertEquals(MinorUnits.ZERO, report.totalIncome)
        assertEquals(MinorUnits(150000L), report.totalExpenses) // Gross expense
        assertEquals(MinorUnits(30000L), report.totalRefunds)   // Refund
        assertEquals(MinorUnits(120000L), report.netExpenses)    // 1500 - 300 = 1200

        // In category breakdown, food expense must be net of refund: 1,200
        val foodCat = report.categoryBreakdown.firstOrNull { it.categoryId == 1L }
        assertEquals(MinorUnits(120000L), foodCat?.amount)
    }

    @Test
    fun `generateReport handles empty transaction history cleanly`() {
        val dateRange = ReportDateRange(
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30),
            periodType = ReportPeriodType.THIS_MONTH,
            label = "September 2026"
        )

        val report = reportService.generateReport(
            dateRange = dateRange,
            allTransactions = emptyList(),
            accounts = listOf(checkingAccount, cashAccount),
            categories = listOf(foodCategory),
            budgets = emptyList(),
            goals = emptyList(),
            treeHistory = emptyList()
        )

        assertEquals(MinorUnits.ZERO, report.totalIncome)
        assertEquals(MinorUnits.ZERO, report.totalExpenses)
        assertEquals(MinorUnits.ZERO, report.netExpenses)
        assertEquals(0.0, report.savingsRatePercent, 0.001)
        assertEquals(0, report.transactionCount)
        assertTrue(report.categoryBreakdown.isEmpty())
        assertEquals(MinorUnits(1200000L), report.openingBalance)
        assertEquals(MinorUnits(1200000L), report.closingBalance)
    }
}
