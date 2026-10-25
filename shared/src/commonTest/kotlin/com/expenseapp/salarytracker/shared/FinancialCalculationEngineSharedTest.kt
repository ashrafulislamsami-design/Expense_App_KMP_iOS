package com.expenseapp.salarytracker.shared

import com.expenseapp.salarytracker.shared.model.Account
import com.expenseapp.salarytracker.shared.model.AccountType
import com.expenseapp.salarytracker.shared.model.Budget
import com.expenseapp.salarytracker.shared.model.BudgetPeriod
import com.expenseapp.salarytracker.shared.model.FinancialDate
import com.expenseapp.salarytracker.shared.model.MinorUnits
import com.expenseapp.salarytracker.shared.model.Transaction
import com.expenseapp.salarytracker.shared.model.TransactionType
import com.expenseapp.salarytracker.shared.service.DateWindow
import com.expenseapp.salarytracker.shared.service.FinancialCalculationEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinancialCalculationEngineSharedTest {

    private val engine = FinancialCalculationEngine()

    @Test
    fun testSummaryDoubleEntryTransferInvariance() {
        val txList = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(500000L), // ৳5,000 income
                type = TransactionType.INCOME,
                accountId = 1,
                date = FinancialDate(2026, 9, 1)
            ),
            Transaction(
                id = 2,
                amount = MinorUnits(150000L), // ৳1,500 expense
                type = TransactionType.EXPENSE,
                accountId = 1,
                date = FinancialDate(2026, 9, 2)
            ),
            Transaction(
                id = 3,
                amount = MinorUnits(200000L), // ৳2,000 transfer between accounts
                type = TransactionType.TRANSFER,
                accountId = 1,
                destinationAccountId = 2,
                date = FinancialDate(2026, 9, 3)
            )
        )

        val summary = engine.calculateSummary(txList)
        assertEquals(500000L, summary.totalIncome.amountInMinorUnits)
        assertEquals(150000L, summary.totalExpense.amountInMinorUnits)
        assertEquals(150000L, summary.netExpense.amountInMinorUnits)
        // Transfers must NEVER distort total income or expense
        assertEquals(350000L, summary.netCashFlow.amountInMinorUnits)
    }

    @Test
    fun testRefundOffsetsExpenseNotGrossIncome() {
        val txList = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(100000L), // ৳1,000 expense
                type = TransactionType.EXPENSE,
                accountId = 1,
                date = FinancialDate(2026, 9, 1)
            ),
            Transaction(
                id = 2,
                amount = MinorUnits(30000L), // ৳300 refund
                type = TransactionType.REFUND,
                accountId = 1,
                date = FinancialDate(2026, 9, 2)
            )
        )

        val summary = engine.calculateSummary(txList)
        assertEquals(100000L, summary.totalExpense.amountInMinorUnits)
        assertEquals(30000L, summary.totalRefunds.amountInMinorUnits)
        assertEquals(70000L, summary.netExpense.amountInMinorUnits)
        assertEquals(0L, summary.totalIncome.amountInMinorUnits)
    }

    @Test
    fun testAccountBalanceCalculations() {
        val account = Account(
            id = 1,
            name = "Cash",
            type = AccountType.CASH,
            initialBalance = MinorUnits(100000L) // ৳1,000 starting
        )

        val txList = listOf(
            Transaction(id = 1, amount = MinorUnits(20000L), type = TransactionType.EXPENSE, accountId = 1, date = FinancialDate(2026, 9, 1)),
            Transaction(id = 2, amount = MinorUnits(50000L), type = TransactionType.INCOME, accountId = 1, date = FinancialDate(2026, 9, 2)),
            Transaction(id = 3, amount = MinorUnits(10000L), type = TransactionType.TRANSFER, accountId = 1, destinationAccountId = 2, date = FinancialDate(2026, 9, 3))
        )

        // 1000 - 200 + 500 - 100 = 1200 (120000L)
        val balance = engine.calculateAccountBalance(account, txList)
        assertEquals(120000L, balance.amountInMinorUnits)
    }

    @Test
    fun testSafeToSpendCalculation() {
        val window = DateWindow(
            startDate = FinancialDate(2026, 9, 1),
            endDate = FinancialDate(2026, 9, 30)
        )
        val budget = Budget(
            id = 1,
            limitAmount = MinorUnits(3000000L), // ৳30,000 limit
            period = BudgetPeriod.MONTHLY
        )

        val txList = listOf(
            Transaction(id = 1, amount = MinorUnits(1000000L), type = TransactionType.EXPENSE, accountId = 1, date = FinancialDate(2026, 9, 5))
        )

        // On Sep 11: 20 days remaining (Sep 11 to Sep 30 inclusive)
        // Remaining budget = 30000 - 10000 = 20000
        // Safe spend = 20000 / 20 = 1000 per day (100000L)
        val estimate = engine.calculateSafeToSpend(
            window = window,
            currentDate = FinancialDate(2026, 9, 11),
            overallBudget = budget,
            salaryIncome = MinorUnits(3000000L),
            periodTransactions = txList
        )

        assertEquals(2000000L, estimate.remainingBudget.amountInMinorUnits)
        assertEquals(20L, estimate.remainingDays)
        assertEquals(100000L, estimate.safeAmountPerDay.amountInMinorUnits)
    }
}
