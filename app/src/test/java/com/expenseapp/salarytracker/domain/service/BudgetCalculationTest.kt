package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetCalculationTest {

    private val engine = FinancialCalculationEngine()

    @Test
    fun testExactlyAtBudget() {
        val budget = Budget(
            id = 1,
            categoryId = 10,
            limitAmount = MinorUnits(500000L), // ৳5,000.00
            warningThresholdPercentage = 80
        )

        val transactions = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(500000L), // exactly ৳5,000.00
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 10,
                date = LocalDate.now()
            )
        )

        val status = engine.calculateBudgetStatus(budget, transactions)

        assertEquals(500000L, status.plannedAmount.amountInMinorUnits)
        assertEquals(500000L, status.actualAmount.amountInMinorUnits)
        assertEquals(0L, status.remainingAmount.amountInMinorUnits)
        assertEquals(0L, status.variance.amountInMinorUnits)
        assertEquals(100.0, status.percentageUsed, 0.001)
        assertFalse("At exactly 100%, it has reached limit but not exceeded it", status.isOverBudget)
    }

    @Test
    fun testOneMinorUnitUnderBudget() {
        val budget = Budget(
            id = 1,
            categoryId = 10,
            limitAmount = MinorUnits(10000L), // ৳100.00 (10,000 cents)
            warningThresholdPercentage = 80
        )

        // ৳99.99 (9,999 cents) -> One minor unit under
        val transactions = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(9999L),
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 10,
                date = LocalDate.now()
            )
        )

        val status = engine.calculateBudgetStatus(budget, transactions)

        assertEquals(10000L, status.plannedAmount.amountInMinorUnits)
        assertEquals(9999L, status.actualAmount.amountInMinorUnits)
        assertEquals(1L, status.remainingAmount.amountInMinorUnits)
        assertEquals(1L, status.variance.amountInMinorUnits) // Positive = under budget
        assertEquals(99.99, status.percentageUsed, 0.001)
        assertFalse(status.isOverBudget)
        assertTrue("At 99.99% with 80% threshold, warning must trigger", status.isWarning)
    }

    @Test
    fun testOneMinorUnitOverBudget() {
        val budget = Budget(
            id = 1,
            categoryId = 10,
            limitAmount = MinorUnits(10000L), // ৳100.00
            warningThresholdPercentage = 80
        )

        // ৳100.01 (10,001 cents) -> One minor unit over
        val transactions = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(10001L),
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 10,
                date = LocalDate.now()
            )
        )

        val status = engine.calculateBudgetStatus(budget, transactions)

        assertEquals(10000L, status.plannedAmount.amountInMinorUnits)
        assertEquals(10001L, status.actualAmount.amountInMinorUnits)
        assertEquals(0L, status.remainingAmount.amountInMinorUnits)
        assertEquals(-1L, status.variance.amountInMinorUnits) // Negative variance = over budget
        assertTrue(status.percentageUsed > 100.0)
        assertTrue(status.isOverBudget)
    }

    @Test
    fun testTransfersExcludedAndRefundsDeductedFromBudget() {
        val budget = Budget(
            id = 1,
            categoryId = 5, // Food & Dining
            limitAmount = MinorUnits(300000L), // ৳3,000.00
            warningThresholdPercentage = 80
        )

        val transactions = listOf(
            // Food Expense: ৳1,500.00
            Transaction(
                id = 1,
                amount = MinorUnits(150000L),
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 5,
                date = LocalDate.now()
            ),
            // Food Refund: ৳200.00
            Transaction(
                id = 2,
                amount = MinorUnits(20000L),
                type = TransactionType.REFUND,
                accountId = 1,
                categoryId = 5,
                date = LocalDate.now()
            ),
            // Transfer: ৳5,000.00 (Must NOT affect budget!)
            Transaction(
                id = 3,
                amount = MinorUnits(500000L),
                type = TransactionType.TRANSFER,
                accountId = 1,
                destinationAccountId = 2,
                date = LocalDate.now()
            )
        )

        val status = engine.calculateBudgetStatus(budget, transactions)

        // Actual spent must be 1,500 - 200 = ৳1,300.00 (130,000 minor units)
        assertEquals(130000L, status.actualAmount.amountInMinorUnits)
        assertEquals(170000L, status.remainingAmount.amountInMinorUnits)
        assertEquals(170000L, status.variance.amountInMinorUnits)
        assertFalse(status.isOverBudget)
    }

    @Test
    fun testOverallSpendingBudgetCalculation() {
        val overallBudget = Budget(
            id = 99,
            categoryId = null, // null = overall budget
            limitAmount = MinorUnits(2000000L), // ৳20,000.00
            period = BudgetPeriod.SALARY_CYCLE,
            warningThresholdPercentage = 80
        )

        val transactions = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(500000L), // ৳5,000
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 1,
                date = LocalDate.now()
            ),
            Transaction(
                id = 2,
                amount = MinorUnits(1200000L), // ৳12,000
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 2,
                date = LocalDate.now()
            ),
            Transaction(
                id = 3,
                amount = MinorUnits(100000L), // ৳1,000 refund
                type = TransactionType.REFUND,
                accountId = 1,
                categoryId = 1,
                date = LocalDate.now()
            )
        )

        val status = engine.calculateBudgetStatus(overallBudget, transactions)

        // Net spent: (5,000 + 12,000) - 1,000 = ৳16,000 (1,600,000 minor units)
        assertEquals(1600000L, status.actualAmount.amountInMinorUnits)
        assertEquals(400000L, status.remainingAmount.amountInMinorUnits)
        assertEquals(80.0, status.percentageUsed, 0.001)
        assertTrue("At 80% with 80% threshold, warning must be true", status.isWarning)
        assertFalse(status.isOverBudget)
    }
}
