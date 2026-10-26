package com.expenseapp.salarytracker.domain.service

import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.MinorUnits
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeState
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class GoalProgress(
    val goal: Goal,
    val currentAmount: MinorUnits,
    val currentDays: Int,
    val targetAmount: MinorUnits,
    val targetDays: Int,
    val progressPercentage: Double, // 0.0 to 100.0+
    val remainingAmount: MinorUnits,
    val remainingDaysToTarget: Int,
    val daysRemainingInGoalWindow: Long,
    val isTargetMet: Boolean,
    val isCeilingExceeded: Boolean, // specifically for SPEND_UNDER and CATEGORY_SPEND_UNDER
    val newMilestonesReached: List<Int>, // Milestones achieved during this calculation not yet in goal.reachedMilestones
    val treeState: TreeState,
    val explanation: String
)

data class TreeOfHopeStatus(
    val state: TreeState,
    val scorePercentage: Double,
    val stageName: String,
    val explanation: String,
    val positiveActionTip: String,
    val contributingFactors: List<String>
)

class GoalCalculationEngine {

    companion object {
        val STANDARD_MILESTONES = listOf(25, 50, 75, 100)
    }

    /**
     * Deterministically calculates the current progress of a Goal based on transaction history.
     */
    fun calculateGoalProgress(
        goal: Goal,
        transactions: Collection<Transaction>,
        today: LocalDate = LocalDate.now()
    ): GoalProgress {
        val windowTxs = transactions.filter { tx ->
            !tx.date.isBefore(goal.startDate) && !tx.date.isAfter(goal.endDate)
        }

        val daysRemainingInWindow = if (today.isAfter(goal.endDate)) {
            0L
        } else {
            val startPoint = if (today.isBefore(goal.startDate)) goal.startDate else today
            ChronoUnit.DAYS.between(startPoint, goal.endDate) + 1
        }

        var currentAmount = MinorUnits.ZERO
        var currentDays = 0
        var isCeilingExceeded = false

        when (goal.type) {
            GoalType.SAVE_AMOUNT -> {
                for (tx in windowTxs) {
                    val accountMatches = goal.accountId == null || tx.accountId == goal.accountId || tx.destinationAccountId == goal.accountId
                    if (accountMatches && tx.type == TransactionType.SAVING) {
                        currentAmount += tx.amount
                    }
                }
            }

            GoalType.SPEND_UNDER -> {
                var rawExpense = MinorUnits.ZERO
                var refunds = MinorUnits.ZERO
                for (tx in windowTxs) {
                    val accountMatches = goal.accountId == null || tx.accountId == goal.accountId
                    if (!accountMatches) continue

                    when (tx.type) {
                        TransactionType.EXPENSE,
                        TransactionType.DONATION -> rawExpense += tx.amount
                        TransactionType.REFUND -> refunds += tx.amount
                        // Transfers strictly excluded
                        TransactionType.TRANSFER,
                        TransactionType.SAVING,
                        TransactionType.DEBT_PAYMENT,
                        TransactionType.DEBT_RECEIVED,
                        TransactionType.INCOME,
                        TransactionType.ADJUSTMENT -> Unit
                    }
                }
                val netSpent = if (rawExpense > refunds) rawExpense - refunds else MinorUnits.ZERO
                currentAmount = netSpent
                isCeilingExceeded = currentAmount > goal.targetAmount
            }

            GoalType.CATEGORY_SPEND_UNDER -> {
                var rawExpense = MinorUnits.ZERO
                var refunds = MinorUnits.ZERO
                for (tx in windowTxs) {
                    if (tx.categoryId != goal.categoryId) continue
                    val accountMatches = goal.accountId == null || tx.accountId == goal.accountId
                    if (!accountMatches) continue

                    when (tx.type) {
                        TransactionType.EXPENSE,
                        TransactionType.DONATION -> rawExpense += tx.amount
                        TransactionType.REFUND -> refunds += tx.amount
                        else -> Unit
                    }
                }
                val netSpent = if (rawExpense > refunds) rawExpense - refunds else MinorUnits.ZERO
                currentAmount = netSpent
                isCeilingExceeded = currentAmount > goal.targetAmount
            }

            GoalType.DONATE_AMOUNT -> {
                for (tx in windowTxs) {
                    val accountMatches = goal.accountId == null || tx.accountId == goal.accountId
                    val categoryMatches = goal.categoryId == null || tx.categoryId == goal.categoryId
                    if (accountMatches && categoryMatches && tx.type == TransactionType.DONATION) {
                        currentAmount += tx.amount
                    }
                }
            }

            GoalType.NO_SPEND -> {
                val effectiveEnd = if (today.isBefore(goal.endDate)) today else goal.endDate
                if (!effectiveEnd.isBefore(goal.startDate)) {
                    var cursor = goal.startDate
                    while (!cursor.isAfter(effectiveEnd)) {
                        val dayTxs = windowTxs.filter { it.date == cursor }
                        var dayExpense = MinorUnits.ZERO
                        var dayRefund = MinorUnits.ZERO
                        for (tx in dayTxs) {
                            val accountMatches = goal.accountId == null || tx.accountId == goal.accountId
                            if (accountMatches) {
                                when (tx.type) {
                                    TransactionType.EXPENSE,
                                    TransactionType.DONATION -> dayExpense += tx.amount
                                    TransactionType.REFUND -> dayRefund += tx.amount
                                    else -> Unit
                                }
                            }
                        }
                        val dayNet = if (dayExpense > dayRefund) dayExpense - dayRefund else MinorUnits.ZERO
                        if (dayNet == MinorUnits.ZERO) {
                            currentDays++
                        }
                        cursor = cursor.plusDays(1)
                    }
                }
            }

            GoalType.STREAK -> {
                // Consecutive tracking streak ending today or at effective end date
                val effectiveEnd = if (today.isBefore(goal.endDate)) today else goal.endDate
                var cursor = effectiveEnd
                while (!cursor.isBefore(goal.startDate)) {
                    val hasActivity = windowTxs.any { it.date == cursor && it.type != TransactionType.TRANSFER }
                    if (hasActivity) {
                        currentDays++
                        cursor = cursor.minusDays(1)
                    } else {
                        // Streak unbroken if it's today and user hasn't logged yet
                        if (cursor == today && currentDays == 0) {
                            cursor = cursor.minusDays(1)
                            continue
                        }
                        break
                    }
                }
            }

            GoalType.CUSTOM -> {
                if (goal.targetAmount.isPositive()) {
                    for (tx in windowTxs) {
                        if (tx.type == TransactionType.SAVING || tx.type == TransactionType.INCOME) {
                            currentAmount += tx.amount
                        }
                    }
                } else if (goal.targetDays > 0) {
                    val days = windowTxs.map { it.date }.distinct().count()
                    currentDays = days
                }
            }
        }

        // Calculate progress percentage and target completion
        val progressPercentage: Double
        val isTargetMet: Boolean
        val remainingAmount: MinorUnits
        val remainingDaysToTarget: Int

        when (goal.type) {
            GoalType.SPEND_UNDER,
            GoalType.CATEGORY_SPEND_UNDER -> {
                if (goal.targetAmount.amountInMinorUnits > 0L) {
                    progressPercentage = (currentAmount.amountInMinorUnits.toDouble() / goal.targetAmount.amountInMinorUnits.toDouble()) * 100.0
                    remainingAmount = if (goal.targetAmount > currentAmount) goal.targetAmount - currentAmount else MinorUnits.ZERO
                } else {
                    progressPercentage = if (currentAmount.isPositive()) 100.0 else 0.0
                    remainingAmount = MinorUnits.ZERO
                }
                remainingDaysToTarget = 0
                isTargetMet = !isCeilingExceeded
            }

            GoalType.NO_SPEND,
            GoalType.STREAK -> {
                progressPercentage = if (goal.targetDays > 0) {
                    (currentDays.toDouble() / goal.targetDays.toDouble()) * 100.0
                } else {
                    0.0
                }
                remainingAmount = MinorUnits.ZERO
                remainingDaysToTarget = maxOf(0, goal.targetDays - currentDays)
                isTargetMet = currentDays >= goal.targetDays && goal.targetDays > 0
            }

            else -> {
                if (goal.targetAmount.amountInMinorUnits > 0L) {
                    progressPercentage = (currentAmount.amountInMinorUnits.toDouble() / goal.targetAmount.amountInMinorUnits.toDouble()) * 100.0
                    remainingAmount = if (goal.targetAmount > currentAmount) goal.targetAmount - currentAmount else MinorUnits.ZERO
                } else {
                    progressPercentage = 0.0
                    remainingAmount = MinorUnits.ZERO
                }
                remainingDaysToTarget = 0
                isTargetMet = currentAmount >= goal.targetAmount && goal.targetAmount.isPositive()
            }
        }

        // Deterministic milestone detection (never re-emits already reached milestones)
        val newMilestonesReached = STANDARD_MILESTONES.filter { milestone ->
            progressPercentage >= milestone && milestone !in goal.reachedMilestones
        }

        val treeState = mapPercentageToTreeState(progressPercentage)

        val explanation = when (goal.type) {
            GoalType.SAVE_AMOUNT -> "${currentAmount.format()} saved toward target of ${goal.targetAmount.format()} (${String.format("%.1f", progressPercentage)}%)."
            GoalType.SPEND_UNDER -> if (isCeilingExceeded) "Spending of ${currentAmount.format()} exceeded limit by ${(-remainingAmount).format()}." else "${currentAmount.format()} spent of ${goal.targetAmount.format()} ceiling. ${remainingAmount.format()} available."
            GoalType.CATEGORY_SPEND_UNDER -> if (isCeilingExceeded) "Category spending of ${currentAmount.format()} exceeded limit." else "${currentAmount.format()} spent of ${goal.targetAmount.format()} limit."
            GoalType.DONATE_AMOUNT -> "${currentAmount.format()} donated of ${goal.targetAmount.format()} target."
            GoalType.NO_SPEND -> "$currentDays no-spend day(s) achieved of ${goal.targetDays} day target."
            GoalType.STREAK -> "$currentDays day discipline streak achieved toward ${goal.targetDays} day target."
            GoalType.CUSTOM -> "Progress: ${String.format("%.1f", progressPercentage)}% achieved."
        }

        return GoalProgress(
            goal = goal,
            currentAmount = currentAmount,
            currentDays = currentDays,
            targetAmount = goal.targetAmount,
            targetDays = goal.targetDays,
            progressPercentage = progressPercentage,
            remainingAmount = remainingAmount,
            remainingDaysToTarget = remainingDaysToTarget,
            daysRemainingInGoalWindow = daysRemainingInWindow,
            isTargetMet = isTargetMet,
            isCeilingExceeded = isCeilingExceeded,
            newMilestonesReached = newMilestonesReached,
            treeState = treeState,
            explanation = explanation
        )
    }

    /**
     * Deterministically maps a completion percentage (0.0 to 100.0+) to a Tree of Hope state.
     */
    fun mapPercentageToTreeState(percentage: Double): TreeState {
        return when {
            percentage < 15.0 -> TreeState.SEED
            percentage < 30.0 -> TreeState.SPROUT
            percentage < 50.0 -> TreeState.YOUNG_PLANT
            percentage < 70.0 -> TreeState.GROWING
            percentage < 85.0 -> TreeState.MATURE
            percentage < 100.0 -> TreeState.FLOWERING
            else -> TreeState.FRUIT_BEARING
        }
    }

    /**
     * Calculates the holistic Tree of Hope status based on active goals and overall financial health.
     * Guaranteed non-shame, constructive language.
     */
    fun calculateTreeOfHopeState(
        activeGoalsWithProgress: List<GoalProgress>,
        overallBudgetStatus: BudgetStatus? = null
    ): TreeOfHopeStatus {
        val contributingFactors = mutableListOf<String>()

        if (activeGoalsWithProgress.isEmpty()) {
            return TreeOfHopeStatus(
                state = TreeState.SEED,
                scorePercentage = 0.0,
                stageName = TreeState.SEED.stageName,
                explanation = "Your savings foundation is established. Create your first savings target or spending limit to build momentum!",
                positiveActionTip = "Tap '+' below to set your first financial goal.",
                contributingFactors = listOf("No active goals currently set")
            )
        }

        // Calculate average goal progress across active goals
        val goalScores = activeGoalsWithProgress.map { gp ->
            if (gp.goal.type == GoalType.SPEND_UNDER || gp.goal.type == GoalType.CATEGORY_SPEND_UNDER) {
                // For spend under, staying under ceiling contributes positively
                if (gp.isCeilingExceeded) 10.0 else maxOf(0.0, 100.0 - gp.progressPercentage)
            } else {
                gp.progressPercentage.coerceIn(0.0, 100.0)
            }
        }
        var aggregateScore = goalScores.average()

        for (gp in activeGoalsWithProgress) {
            contributingFactors.add("${gp.goal.name}: ${String.format("%.1f", gp.progressPercentage)}% progress")
        }

        // Budget health factor
        if (overallBudgetStatus != null) {
            if (overallBudgetStatus.isOverBudget) {
                aggregateScore = (aggregateScore * 0.8).coerceAtLeast(10.0)
                contributingFactors.add("Master cycle budget is over limit")
            } else if (overallBudgetStatus.isWarning) {
                contributingFactors.add("Master cycle budget is near limit (${String.format("%.0f", overallBudgetStatus.percentageUsed)}%)")
            } else {
                aggregateScore = (aggregateScore * 1.05).coerceAtMost(100.0)
                contributingFactors.add("Master cycle budget is healthy")
            }
        }

        val state = mapPercentageToTreeState(aggregateScore)

        val positiveTip = when (state) {
            TreeState.SEED -> "Log a saving transaction or complete a no-spend day to sprout your tree."
            TreeState.SPROUT -> "Maintain daily tracking discipline to grow into a young sapling."
            TreeState.YOUNG_PLANT -> "Crossing the 50% milestone on your goals will grow your tree strong."
            TreeState.GROWING -> "Stay within your cycle budget to reach full maturity."
            TreeState.MATURE -> "You're close to full bloom! Keep up consistent habits."
            TreeState.FLOWERING -> "Final push: complete your remaining target to bear fruit."
            TreeState.FRUIT_BEARING -> "Incredible discipline! Your financial runway and savings habits are at peak strength."
        }

        val explanation = when (state) {
            TreeState.SEED -> "Tree is a Seed. Initial targets are being set."
            TreeState.SPROUT -> "Tree has Sprouted based on early goal progress."
            TreeState.YOUNG_PLANT -> "Tree is a Young Plant with consistent contributions."
            TreeState.GROWING -> "Tree is Growing steadily past the halfway milestone."
            TreeState.MATURE -> "Tree is Mature, reflecting disciplined spending control."
            TreeState.FLOWERING -> "Tree is Flowering as targets approach completion."
            TreeState.FRUIT_BEARING -> "Tree is Fruit-Bearing! You have fulfilled your financial goals."
        }

        return TreeOfHopeStatus(
            state = state,
            scorePercentage = aggregateScore,
            stageName = state.stageName,
            explanation = explanation,
            positiveActionTip = positiveTip,
            contributingFactors = contributingFactors
        )
    }
}
