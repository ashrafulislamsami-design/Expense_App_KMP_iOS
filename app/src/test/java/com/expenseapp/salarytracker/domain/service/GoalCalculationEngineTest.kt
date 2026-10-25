package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalCalculationEngineTest {

    private val engine = GoalCalculationEngine()
    private val today = LocalDate.of(2026, 9, 30)

    // 1. SAVE_AMOUNT
    @Test
    fun test1_SaveAmountGoal_TracksSavingsTransactions() {
        val goal = Goal(
            id = 1,
            name = "Emergency Fund",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(1000000L), // ৳10,000.00
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(400000L), // ৳4,000.00
                type = TransactionType.SAVING,
                accountId = 1,
                date = LocalDate.of(2026, 9, 10)
            ),
            Transaction(
                id = 2,
                amount = MinorUnits(200000L), // ৳2,000.00
                type = TransactionType.SAVING,
                accountId = 1,
                date = LocalDate.of(2026, 9, 15)
            )
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(600000L, progress.currentAmount.amountInMinorUnits) // ৳6,000.00
        assertEquals(400000L, progress.remainingAmount.amountInMinorUnits)
        assertEquals(60.0, progress.progressPercentage, 0.001)
        assertFalse(progress.isTargetMet)
        assertEquals(TreeState.GROWING, progress.treeState)
    }

    // 2. SPEND_UNDER
    @Test
    fun test2_SpendUnderGoal_TracksSpendingAgainstCeiling() {
        val goal = Goal(
            id = 2,
            name = "Overall Spending Ceiling",
            type = GoalType.SPEND_UNDER,
            targetAmount = MinorUnits(500000L), // ৳5,000.00 ceiling
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(350000L), // ৳3,500.00 spent
                type = TransactionType.EXPENSE,
                accountId = 1,
                date = LocalDate.of(2026, 9, 5)
            )
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(350000L, progress.currentAmount.amountInMinorUnits)
        assertEquals(150000L, progress.remainingAmount.amountInMinorUnits) // ৳1,500.00 ceiling room left
        assertEquals(70.0, progress.progressPercentage, 0.001)
        assertFalse(progress.isCeilingExceeded)
        assertTrue(progress.isTargetMet)
    }

    // 3. CATEGORY_SPEND_UNDER
    @Test
    fun test3_CategorySpendUnder_FiltersOnlyCategoryTransactions() {
        val goal = Goal(
            id = 3,
            name = "Dining Ceiling",
            type = GoalType.CATEGORY_SPEND_UNDER,
            targetAmount = MinorUnits(200000L), // ৳2,000.00
            categoryId = 10,
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            // Category 10 expense
            Transaction(
                id = 1,
                amount = MinorUnits(120000L), // ৳1,200
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 10,
                date = LocalDate.of(2026, 9, 10)
            ),
            // Unrelated Category 20 expense (Must NOT be counted!)
            Transaction(
                id = 2,
                amount = MinorUnits(800000L), // ৳8,000
                type = TransactionType.EXPENSE,
                accountId = 1,
                categoryId = 20,
                date = LocalDate.of(2026, 9, 12)
            )
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(120000L, progress.currentAmount.amountInMinorUnits)
        assertEquals(80000L, progress.remainingAmount.amountInMinorUnits)
        assertFalse(progress.isCeilingExceeded)
    }

    // 4. DONATE_AMOUNT
    @Test
    fun test4_DonateAmount_TracksDonationsOnly() {
        val goal = Goal(
            id = 4,
            name = "Charity Target",
            type = GoalType.DONATE_AMOUNT,
            targetAmount = MinorUnits(100000L), // ৳1,000.00
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(
                id = 1,
                amount = MinorUnits(100000L),
                type = TransactionType.DONATION,
                accountId = 1,
                date = LocalDate.of(2026, 9, 15)
            ),
            Transaction(
                id = 2,
                amount = MinorUnits(50000L),
                type = TransactionType.EXPENSE, // Regular expense should not count as donation
                accountId = 1,
                date = LocalDate.of(2026, 9, 16)
            )
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(100000L, progress.currentAmount.amountInMinorUnits)
        assertEquals(100.0, progress.progressPercentage, 0.001)
        assertTrue(progress.isTargetMet)
    }

    // 5. NO_SPEND
    @Test
    fun test5_NoSpendGoal_CountsCleanDaysWithoutExpenses() {
        val goal = Goal(
            id = 5,
            name = "10 Days No-Spend Challenge",
            type = GoalType.NO_SPEND,
            targetDays = 10,
            startDate = LocalDate.of(2026, 9, 21),
            endDate = LocalDate.of(2026, 9, 30) // 10 days total
        )

        // Day 21, 22, 23: spent money
        // Day 24, 25, 26, 27, 28, 29, 30: zero spending (7 clean days)
        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(5000L), type = TransactionType.EXPENSE, accountId = 1, date = LocalDate.of(2026, 9, 21)),
            Transaction(id = 2, amount = MinorUnits(3000L), type = TransactionType.EXPENSE, accountId = 1, date = LocalDate.of(2026, 9, 22)),
            Transaction(id = 3, amount = MinorUnits(2000L), type = TransactionType.EXPENSE, accountId = 1, date = LocalDate.of(2026, 9, 23))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(7, progress.currentDays) // 7 no-spend days out of 10
        assertEquals(70.0, progress.progressPercentage, 0.001)
        assertEquals(3, progress.remainingDaysToTarget)
        assertFalse(progress.isTargetMet)
    }

    // 6. STREAK
    @Test
    fun test6_StreakGoal_TracksConsecutiveDailyTracking() {
        val goal = Goal(
            id = 6,
            name = "5-Day Streak",
            type = GoalType.STREAK,
            targetDays = 5,
            startDate = LocalDate.of(2026, 9, 20),
            endDate = LocalDate.of(2026, 9, 30)
        )

        // Continuous activity on Sep 28, 29, 30 (3-day active streak)
        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(1000L), type = TransactionType.EXPENSE, accountId = 1, date = LocalDate.of(2026, 9, 28)),
            Transaction(id = 2, amount = MinorUnits(2000L), type = TransactionType.EXPENSE, accountId = 1, date = LocalDate.of(2026, 9, 29)),
            Transaction(id = 3, amount = MinorUnits(3000L), type = TransactionType.EXPENSE, accountId = 1, date = LocalDate.of(2026, 9, 30))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(3, progress.currentDays)
        assertEquals(60.0, progress.progressPercentage, 0.001)
        assertEquals(2, progress.remainingDaysToTarget)
    }

    // 7. CUSTOM
    @Test
    fun test7_CustomGoal_HandlesExtensibleTarget() {
        val goal = Goal(
            id = 7,
            name = "Custom Savings Plan",
            type = GoalType.CUSTOM,
            targetAmount = MinorUnits(50000L), // ৳500.00
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(25000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 15))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(25000L, progress.currentAmount.amountInMinorUnits)
        assertEquals(50.0, progress.progressPercentage, 0.001)
    }

    // 8. ZERO TRANSACTIONS
    @Test
    fun test8_ZeroTransactions_ReturnsZeroProgressSafely() {
        val goal = Goal(
            id = 8,
            name = "Empty Goal",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(500000L),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val progress = engine.calculateGoalProgress(goal, emptyList(), today)

        assertEquals(0L, progress.currentAmount.amountInMinorUnits)
        assertEquals(500000L, progress.remainingAmount.amountInMinorUnits)
        assertEquals(0.0, progress.progressPercentage, 0.001)
        assertFalse(progress.isTargetMet)
        assertEquals(TreeState.SEED, progress.treeState)
    }

    // 9. REFUNDS
    @Test
    fun test9_RefundsReduceSpendUnderAmounts() {
        val goal = Goal(
            id = 9,
            name = "Shopping Limit",
            type = GoalType.CATEGORY_SPEND_UNDER,
            targetAmount = MinorUnits(100000L), // ৳1,000 ceiling
            categoryId = 5,
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(70000L), type = TransactionType.EXPENSE, accountId = 1, categoryId = 5, date = LocalDate.of(2026, 9, 10)),
            Transaction(id = 2, amount = MinorUnits(20000L), type = TransactionType.REFUND, accountId = 1, categoryId = 5, date = LocalDate.of(2026, 9, 15))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        // Net spent: 700 - 200 = ৳500 (50,000 minor units)
        assertEquals(50000L, progress.currentAmount.amountInMinorUnits)
        assertEquals(50000L, progress.remainingAmount.amountInMinorUnits)
        assertEquals(50.0, progress.progressPercentage, 0.001)
        assertFalse(progress.isCeilingExceeded)
    }

    // 10. TRANSFERS
    @Test
    fun test10_TransfersNeverCountTowardSpendingGoals() {
        val goal = Goal(
            id = 10,
            name = "Strict Limit",
            type = GoalType.SPEND_UNDER,
            targetAmount = MinorUnits(100000L), // ৳1,000 limit
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(5000000L), type = TransactionType.TRANSFER, accountId = 1, destinationAccountId = 2, date = LocalDate.of(2026, 9, 10))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        // Transfer must be completely ignored -> 0 spent
        assertEquals(0L, progress.currentAmount.amountInMinorUnits)
        assertEquals(0.0, progress.progressPercentage, 0.001)
        assertFalse(progress.isCeilingExceeded)
    }

    // 11. MULTIPLE ACCOUNTS
    @Test
    fun test11_MultipleAccounts_FiltersLinkedAccountCorrectly() {
        val goal = Goal(
            id = 11,
            name = "Bank Savings",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(100000L),
            accountId = 2, // only Account 2
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(50000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 10)),
            Transaction(id = 2, amount = MinorUnits(60000L), type = TransactionType.SAVING, accountId = 2, date = LocalDate.of(2026, 9, 12))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        // Only Account 2 saving (৳600.00) must count
        assertEquals(60000L, progress.currentAmount.amountInMinorUnits)
        assertEquals(60.0, progress.progressPercentage, 0.001)
    }

    // 12 & 13. SALARY CYCLE & CALENDAR MONTH BOUNDARIES
    @Test
    fun test12_13_DateWindowBoundaries_OnlyCountsTransactionsWithinBounds() {
        val goal = Goal(
            id = 12,
            name = "Payday-to-Payday Savings",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(200000L),
            startDate = LocalDate.of(2026, 9, 25),
            endDate = LocalDate.of(2026, 10, 24)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(50000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 24)), // Before start -> Ignored
            Transaction(id = 2, amount = MinorUnits(100000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 25)), // On start -> Counted
            Transaction(id = 3, amount = MinorUnits(80000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 10, 24)), // On end -> Counted
            Transaction(id = 4, amount = MinorUnits(70000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 10, 25))  // After end -> Ignored
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        // Counted: 100,000 + 80,000 = 180,000 minor units
        assertEquals(180000L, progress.currentAmount.amountInMinorUnits)
        assertEquals(90.0, progress.progressPercentage, 0.001)
    }

    // 14 & 15. START AND END DATE
    @Test
    fun test14_15_GoalStartAndEndDate_RemainingDaysCalculatedProperly() {
        val goal = Goal(
            id = 14,
            name = "Quarterly Goal",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(1000000L),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 10, 10)
        )

        // Today is Sep 30: Sep 30 to Oct 10 inclusive is 11 days
        val progress = engine.calculateGoalProgress(goal, emptyList(), today)
        assertEquals(11L, progress.daysRemainingInGoalWindow)
    }

    // 16. EXACTLY AT TARGET
    @Test
    fun test16_ExactlyAtTarget_Reaches100PercentAndMeetsTarget() {
        val goal = Goal(
            id = 16,
            name = "Vacation Fund",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(500000L),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(500000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 15))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(500000L, progress.currentAmount.amountInMinorUnits)
        assertEquals(0L, progress.remainingAmount.amountInMinorUnits)
        assertEquals(100.0, progress.progressPercentage, 0.001)
        assertTrue(progress.isTargetMet)
        assertEquals(TreeState.FRUIT_BEARING, progress.treeState)
    }

    // 17. ONE MINOR UNIT BELOW TARGET
    @Test
    fun test17_OneMinorUnitBelowTarget_DoesNotTrigger100PercentTargetMet() {
        val goal = Goal(
            id = 17,
            name = "Savings",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(10000L), // ৳100.00
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(9999L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 15))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(9999L, progress.currentAmount.amountInMinorUnits)
        assertEquals(1L, progress.remainingAmount.amountInMinorUnits)
        assertEquals(99.99, progress.progressPercentage, 0.001)
        assertFalse(progress.isTargetMet)
        assertEquals(TreeState.FLOWERING, progress.treeState)
    }

    // 18. ONE MINOR UNIT ABOVE TARGET
    @Test
    fun test18_OneMinorUnitAboveTarget_TriggersTargetMetAndFruitBearing() {
        val goal = Goal(
            id = 18,
            name = "Savings",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(10000L),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(10001L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 15))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(10001L, progress.currentAmount.amountInMinorUnits)
        assertEquals(0L, progress.remainingAmount.amountInMinorUnits)
        assertTrue(progress.progressPercentage > 100.0)
        assertTrue(progress.isTargetMet)
        assertEquals(TreeState.FRUIT_BEARING, progress.treeState)
    }

    // 19. MILESTONE REACHED EXACTLY
    @Test
    fun test19_MilestoneReachedExactly_DetectsNewMilestone() {
        val goal = Goal(
            id = 19,
            name = "Laptop Savings",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(100000L),
            reachedMilestones = emptyList(),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(25000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 15))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        // Exactly 25% reached!
        assertEquals(listOf(25), progress.newMilestonesReached)
    }

    // 20. MILESTONE NOT REPEATEDLY EMITTED
    @Test
    fun test20_MilestoneNotRepeatedlyEmitted_ExcludesAlreadyReachedMilestones() {
        val goal = Goal(
            id = 20,
            name = "Laptop Savings",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(100000L),
            reachedMilestones = listOf(25, 50), // Already reached 25 and 50
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        // Progress advances to 55%
        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(55000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 15))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)

        // 25 and 50 must NOT be in newMilestonesReached!
        assertTrue(progress.newMilestonesReached.isEmpty())

        // Now progress advances to 80% (crosses 75%)
        val txs2 = listOf(
            Transaction(id = 1, amount = MinorUnits(80000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 15))
        )
        val progress2 = engine.calculateGoalProgress(goal, txs2, today)
        assertEquals(listOf(75), progress2.newMilestonesReached)
    }

    // 21. TREE STATE BOUNDARIES
    @Test
    fun test21_TreeStateBoundaries() {
        assertEquals(TreeState.SEED, engine.mapPercentageToTreeState(0.0))
        assertEquals(TreeState.SEED, engine.mapPercentageToTreeState(14.9))
        assertEquals(TreeState.SPROUT, engine.mapPercentageToTreeState(15.0))
        assertEquals(TreeState.SPROUT, engine.mapPercentageToTreeState(29.9))
        assertEquals(TreeState.YOUNG_PLANT, engine.mapPercentageToTreeState(30.0))
        assertEquals(TreeState.YOUNG_PLANT, engine.mapPercentageToTreeState(49.9))
        assertEquals(TreeState.GROWING, engine.mapPercentageToTreeState(50.0))
        assertEquals(TreeState.GROWING, engine.mapPercentageToTreeState(69.9))
        assertEquals(TreeState.MATURE, engine.mapPercentageToTreeState(70.0))
        assertEquals(TreeState.MATURE, engine.mapPercentageToTreeState(84.9))
        assertEquals(TreeState.FLOWERING, engine.mapPercentageToTreeState(85.0))
        assertEquals(TreeState.FLOWERING, engine.mapPercentageToTreeState(99.9))
        assertEquals(TreeState.FRUIT_BEARING, engine.mapPercentageToTreeState(100.0))
        assertEquals(TreeState.FRUIT_BEARING, engine.mapPercentageToTreeState(150.0))
    }

    // 22. TREE STATE TRANSITIONS & NON-SHAME EXPLANATIONS
    @Test
    fun test22_TreeStateTransitionsAndConstructiveGuidance() {
        val goal = Goal(
            id = 22,
            name = "Education Fund",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(100000L),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(35000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 10))
        )

        val progress = engine.calculateGoalProgress(goal, txs, today)
        val treeStatus = engine.calculateTreeOfHopeState(listOf(progress), null)

        assertEquals(TreeState.YOUNG_PLANT, treeStatus.state)
        assertEquals("Young Plant", treeStatus.stageName)
        assertTrue(treeStatus.positiveActionTip.isNotEmpty())
        assertFalse("Avoid shame words", treeStatus.explanation.contains("failed", ignoreCase = true))
    }

    // 23. RECALCULATION FROM TRANSACTION HISTORY
    @Test
    fun test23_RecalculationFromTransactionHistory() {
        val goal = Goal(
            id = 23,
            name = "Emergency Savings",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(100000L),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val initialTxs = listOf(
            Transaction(id = 1, amount = MinorUnits(20000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 5))
        )
        val initialProgress = engine.calculateGoalProgress(goal, initialTxs, today)
        assertEquals(20000L, initialProgress.currentAmount.amountInMinorUnits)

        // Retroactive transaction added 3 weeks ago
        val updatedTxs = initialTxs + listOf(
            Transaction(id = 2, amount = MinorUnits(30000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 8))
        )
        val recomputedProgress = engine.calculateGoalProgress(goal, updatedTxs, today)
        assertEquals(50000L, recomputedProgress.currentAmount.amountInMinorUnits)
        assertEquals(50.0, recomputedProgress.progressPercentage, 0.001)
    }

    // 24. CACHED PROGRESS CONSISTENCY
    @Test
    fun test24_CachedProgressConsistency_DeterministicRebuild() {
        val goal = Goal(
            id = 24,
            name = "Consistently Computed Goal",
            type = GoalType.SAVE_AMOUNT,
            targetAmount = MinorUnits(500000L),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = LocalDate.of(2026, 9, 30)
        )

        val txs = listOf(
            Transaction(id = 1, amount = MinorUnits(125000L), type = TransactionType.SAVING, accountId = 1, date = LocalDate.of(2026, 9, 10))
        )

        // Multiple independent runs on identical inputs must be bit-for-bit identical
        val run1 = engine.calculateGoalProgress(goal, txs, today)
        val run2 = engine.calculateGoalProgress(goal, txs, today)

        assertEquals(run1.currentAmount, run2.currentAmount)
        assertEquals(run1.progressPercentage, run2.progressPercentage, 0.0)
        assertEquals(run1.treeState, run2.treeState)
        assertEquals(run1.isTargetMet, run2.isTargetMet)
        assertEquals(run1.explanation, run2.explanation)
    }
}
