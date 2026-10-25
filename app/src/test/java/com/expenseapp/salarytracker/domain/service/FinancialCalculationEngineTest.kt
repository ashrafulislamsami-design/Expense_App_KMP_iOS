package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FinancialCalculationEngineTest {

    private val engine = FinancialCalculationEngine()

    @Test
    fun testTransferInvariance_TransfersNeverCountAsExpensesOrIncome() {
        val transactions = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(500000L), // ৳5,000.00
                type = TransactionType.INCOME,
                accountId = 1,
                date = LocalDate.now()
            ),
            Transaction(
                id = 2,
                amount = MinorUnits(120000L), // ৳1,200.00
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 1,
                date = LocalDate.now()
            ),
            Transaction(
                id = 3,
                amount = MinorUnits(200000L), // ৳2,000.00 transfer from Account 1 to Account 2
                type = TransactionType.TRANSFER,
                accountId = 1,
                destinationAccountId = 2,
                date = LocalDate.now()
            )
        )

        val summary = engine.calculateSummary(transactions)

        // Income must be exactly ৳5,000.00
        assertEquals(500000L, summary.totalIncome.amountInMinorUnits)
        // Expense must be exactly ৳1,200.00 (Transfer must NOT be counted as expense)
        assertEquals(120000L, summary.totalExpense.amountInMinorUnits)
        assertEquals(120000L, summary.netExpense.amountInMinorUnits)
    }

    @Test
    fun testRefundsReduceExpenses_DoNotInflateIncome() {
        val transactions = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(100000L), // ৳1,000.00 income
                type = TransactionType.INCOME,
                accountId = 1,
                date = LocalDate.now()
            ),
            Transaction(
                id = 2,
                amount = MinorUnits(30000L), // ৳300.00 expense on Shopping
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 8,
                date = LocalDate.now()
            ),
            Transaction(
                id = 3,
                amount = MinorUnits(5000L), // ৳50.00 refund on Shopping
                type = TransactionType.REFUND,
                accountId = 1,
                categoryId = 8,
                date = LocalDate.now()
            )
        )

        val summary = engine.calculateSummary(transactions)

        // Income must remain ৳1,000.00 (Refund is NOT income)
        assertEquals(100000L, summary.totalIncome.amountInMinorUnits)
        // Raw expense was ৳300.00
        assertEquals(30000L, summary.totalExpense.amountInMinorUnits)
        // Total refund was ৳50.00
        assertEquals(5000L, summary.totalRefunds.amountInMinorUnits)
        // Net expense must be ৳250.00
        assertEquals(25000L, summary.netExpense.amountInMinorUnits)
        // Net change: 1000 - 250 = ৳750.00
        assertEquals(75000L, summary.netChange.amountInMinorUnits)
    }

    @Test
    fun testDoubleEntryAccountBalances() {
        val bankAccount = Account(id = 1, name = "Bank", type = AccountType.BANK, initialBalance = MinorUnits(1000000L)) // ৳10,000
        val cashAccount = Account(id = 2, name = "Cash", type = AccountType.CASH, initialBalance = MinorUnits(50000L))   // ৳500

        val transactions = listOf(
            // Transfer ৳2,000 from Bank to Cash
            Transaction(
                id = 1,
                amount = MinorUnits(200000L),
                type = TransactionType.TRANSFER,
                accountId = 1,
                destinationAccountId = 2,
                date = LocalDate.now()
            ),
            // Cash expense ৳300
            Transaction(
                id = 2,
                amount = MinorUnits(30000L),
                type = TransactionType.EXPENSE,
                accountId = 2,
                date = LocalDate.now()
            )
        )

        // Bank balance: 10,000 - 2,000 = ৳8,000
        val bankBalance = engine.calculateAccountBalance(bankAccount, transactions)
        assertEquals(800000L, bankBalance.amountInMinorUnits)

        // Cash balance: 500 + 2,000 - 300 = ৳2,200
        val cashBalance = engine.calculateAccountBalance(cashAccount, transactions)
        assertEquals(220000L, cashBalance.amountInMinorUnits)
    }

    @Test
    fun testSafeToSpendCalculation() {
        val window = DateWindow(
            startDate = LocalDate.of(2026, 9, 25),
            endDate = LocalDate.of(2026, 10, 24) // 30 days total
        )
        // Today is 29 September (26 days remaining inclusive of today)
        val today = LocalDate.of(2026, 9, 29)

        val salaryIncome = MinorUnits(5200000L) // ৳52,000

        val txs = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(520000L), // ৳5,200 already spent
                type = TransactionType.EXPENSE,
                accountId = 1,
                date = today
            )
        )

        // Remaining: 52,000 - 5,200 = ৳46,800
        // Remaining days: 26
        // Safe to spend per day: 46,800 / 26 = ৳1,800.00
        val estimate = engine.calculateSafeToSpend(
            window = window,
            currentDate = today,
            overallBudget = null,
            salaryIncome = salaryIncome,
            periodTransactions = txs
        )

        assertEquals(26L, estimate.remainingDays)
        assertEquals(4680000L, estimate.remainingBudget.amountInMinorUnits)
        assertEquals(180000L, estimate.safeAmountPerDay.amountInMinorUnits) // ৳1,800.00 exactly
    }
}
