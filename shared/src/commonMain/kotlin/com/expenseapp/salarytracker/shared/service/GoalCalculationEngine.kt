package com.expenseapp.salarytracker.shared.service

import com.expenseapp.salarytracker.shared.model.FinancialDate
import com.expenseapp.salarytracker.shared.model.Goal
import com.expenseapp.salarytracker.shared.model.GoalStatus
import com.expenseapp.salarytracker.shared.model.GoalType
import com.expenseapp.salarytracker.shared.model.MinorUnits
import com.expenseapp.salarytracker.shared.model.Transaction
import com.expenseapp.salarytracker.shared.model.TransactionType
import com.expenseapp.salarytracker.shared.model.TreeState

data class GoalProgress(
    val goal: Goal,
    val currentAmount: MinorUnits,
    val percentageAchieved: Double,
    val daysRemaining: Long,
    val isAheadOfSchedule: Boolean,
    val newlyUnlockedMilestones: List<Int>
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

    /**
     * Calculates the deterministic progress of a goal based on transaction history.
     */
    fun calculateGoalProgress(
        goal: Goal,
        allTransactions: Collection<Transaction>,
        currentDate: FinancialDate
    ): GoalProgress {
        val relevantTx = allTransactions.filter { tx ->
            !tx.date.isBefore(goal.startDate) && !tx.date.isAfter(goal.endDate)
        }

        val currentAmount = when (goal.type) {
            GoalType.SAVE_AMOUNT -> {
                val savingsTx = relevantTx.filter {
                    it.type == TransactionType.SAVING &&
                    (goal.accountId == null || it.accountId == goal.accountId || it.destinationAccountId == goal.accountId)
                }
                savingsTx.fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
            }
            GoalType.SPEND_UNDER -> {
                val expenseTx = relevantTx.filter { it.type == TransactionType.EXPENSE }
                expenseTx.fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
            }
            GoalType.CATEGORY_SPEND_UNDER -> {
                val catExpense = relevantTx.filter {
                    it.type == TransactionType.EXPENSE && it.categoryId == goal.categoryId
                }
                catExpense.fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
            }
            GoalType.DONATE_AMOUNT -> {
                val donateTx = relevantTx.filter { it.type == TransactionType.DONATION }
                donateTx.fold(MinorUnits.ZERO) { acc, tx -> acc + tx.amount }
            }
            GoalType.NO_SPEND, GoalType.STREAK, GoalType.CUSTOM -> {
                MinorUnits.ZERO
            }
        }

        val pct = if (goal.targetAmount.isPositive()) {
            ((currentAmount.amountInMinorUnits.toDouble() / goal.targetAmount.amountInMinorUnits.toDouble()) * 100.0)
                .coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val totalGoalDays = FinancialDate.daysBetween(goal.startDate, goal.endDate) + 1
        val daysPassed = if (currentDate.isBefore(goal.startDate)) 0L
        else FinancialDate.daysBetween(goal.startDate, currentDate) + 1
        val expectedPct = if (totalGoalDays > 0) ((daysPassed.toDouble() / totalGoalDays.toDouble()) * 100.0) else 0.0

        val daysRemaining = if (currentDate.isAfter(goal.endDate)) 0L
        else FinancialDate.daysBetween(currentDate, goal.endDate)

        val milestones = listOf(25, 50, 75, 100)
        val newlyUnlocked = milestones.filter { m ->
            pct >= m && !goal.reachedMilestones.contains(m)
        }

        val isAhead = pct >= expectedPct

        return GoalProgress(
            goal = goal,
            currentAmount = currentAmount,
            percentageAchieved = pct,
            daysRemaining = daysRemaining,
            isAheadOfSchedule = isAhead,
            newlyUnlockedMilestones = newlyUnlocked
        )
    }

    /**
     * Tree of Hope vitality health calculation (0.0% to 100.0%).
     */
    fun calculateTreeOfHopeState(
        activeGoalsWithProgress: List<GoalProgress>,
        overallBudgetStatus: BudgetStatus?
    ): TreeOfHopeStatus {
        val factors = mutableListOf<String>()
        var score = 50.0 // Baseline vitality

        if (overallBudgetStatus != null) {
            when {
                overallBudgetStatus.isOverBudget -> {
                    score -= 35.0
                    factors.add("Over overall spending budget limit (-35%)")
                }
                overallBudgetStatus.isWarning -> {
                    score -= 10.0
                    factors.add("Approaching budget ceiling warning threshold (-10%)")
                }
                else -> {
                    score += 20.0
                    factors.add("Spending is safely within cycle budget (+20%)")
                }
            }
        } else {
            factors.add("No overall budget limit active (neutral baseline)")
        }

        if (activeGoalsWithProgress.isNotEmpty()) {
            val avgAchieved = activeGoalsWithProgress.map { it.percentageAchieved }.average()
            val bonus = (avgAchieved * 0.30).coerceIn(0.0, 30.0)
            score += bonus
            factors.add("Active goal achievement pace: +${bonus.toInt()}%")

            val completedCount = activeGoalsWithProgress.count { it.percentageAchieved >= 100.0 }
            if (completedCount > 0) {
                score += completedCount * 10.0
                factors.add("$completedCount goal(s) fully achieved (+${completedCount * 10}%)")
            }
        }

        val clampedScore = score.coerceIn(0.0, 100.0)
        val treeState = when {
            clampedScore >= 90.0 -> TreeState.FRUIT_BEARING
            clampedScore >= 75.0 -> TreeState.FLOWERING
            clampedScore >= 60.0 -> TreeState.MATURE
            clampedScore >= 45.0 -> TreeState.GROWING
            clampedScore >= 30.0 -> TreeState.YOUNG_PLANT
            clampedScore >= 15.0 -> TreeState.SPROUT
            else -> TreeState.SEED
        }

        val (explanation, tip) = when (treeState) {
            TreeState.FRUIT_BEARING -> Pair(
                "Your Tree of Hope is flourishing and bearing fruit! Financial discipline is exceptional.",
                "Consider setting a new ambitious milestone or building your emergency buffer."
            )
            TreeState.FLOWERING -> Pair(
                "Your Tree is in full bloom. You are maintaining healthy spending and steady savings.",
                "Keep daily spending below the safe-to-spend pace to harvest rewards."
            )
            TreeState.MATURE -> Pair(
                "Your Tree is strong and mature with deep financial roots.",
                "Continue tracking everyday transactions diligently."
            )
            TreeState.GROWING -> Pair(
                "Your Tree is actively growing healthy green branches.",
                "Stay mindful of impulse purchases over the weekend."
            )
            TreeState.YOUNG_PLANT -> Pair(
                "A young plant has formed and needs consistent care.",
                "Check your daily safe-to-spend allowance before discretionary shopping."
            )
            TreeState.SPROUT -> Pair(
                "A delicate sprout has emerged. Budget pressure is currently testing its growth.",
                "Review recent expenses and identify one area to trim down."
            )
            TreeState.SEED -> Pair(
                "Your financial tree is currently a dormant seed waiting for nurturing conditions.",
                "Record your expenses and stay within budget to help your seed sprout."
            )
        }

        return TreeOfHopeStatus(
            state = treeState,
            scorePercentage = clampedScore,
            stageName = treeState.stageName,
            explanation = explanation,
            positiveActionTip = tip,
            contributingFactors = factors
        )
    }
}
