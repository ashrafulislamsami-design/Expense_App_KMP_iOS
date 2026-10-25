package com.expenseapp.salarytracker.domain.model

import java.time.LocalDate

enum class GoalType(val displayName: String) {
    SAVE_AMOUNT("Save Target Amount"),
    SPEND_UNDER("Spend Under Overall Limit"),
    CATEGORY_SPEND_UNDER("Keep Category Spending Under"),
    DONATE_AMOUNT("Donate Target Amount"),
    NO_SPEND("No Spend Challenge"),
    STREAK("Daily Expense Tracking Streak"),
    CUSTOM("Custom Goal");

    companion object {
        fun fromString(value: String): GoalType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SAVE_AMOUNT
    }
}

enum class GoalStatus(val displayName: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    ARCHIVED("Archived");

    companion object {
        fun fromString(value: String?): GoalStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
    }
}

enum class TreeState(val stageName: String, val level: Int) {
    SEED("Seed", 1),
    SPROUT("Sprout", 2),
    YOUNG_PLANT("Young Plant", 3),
    GROWING("Growing", 4),
    MATURE("Mature", 5),
    FLOWERING("Flowering", 6),
    FRUIT_BEARING("Fruit-Bearing", 7);

    val displayName: String get() = stageName

    companion object {
        fun fromString(value: String?): TreeState {
            if (value == null) return SEED
            return when (value.trim().uppercase()) {
                "SEED" -> SEED
                "SPROUT" -> SPROUT
                "YOUNG_PLANT" -> YOUNG_PLANT
                "GROWING", "GROWING_TREE" -> GROWING
                "MATURE", "MATURE_TREE" -> MATURE
                "FLOWERING", "FLOWERING_TREE" -> FLOWERING
                "FRUIT_BEARING", "FRUIT_BEARING_TREE" -> FRUIT_BEARING
                else -> SEED
            }
        }
    }
}

data class Goal(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: GoalType,
    val targetAmount: MinorUnits = MinorUnits.ZERO,
    val targetDays: Int = 0,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val reachedMilestones: List<Int> = emptyList(), // e.g. [25, 50, 75, 100]
    val treeState: TreeState = TreeState.SEED,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = createdAtEpochMs
) {
    // Backward-compatibility properties
    val isCompleted: Boolean get() = status == GoalStatus.COMPLETED
    val isFailed: Boolean get() = status == GoalStatus.ARCHIVED
}

data class TreeHistory(
    val id: Long = 0,
    val previousState: TreeState,
    val newState: TreeState,
    val reason: String,
    val contributingGoalId: Long? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
