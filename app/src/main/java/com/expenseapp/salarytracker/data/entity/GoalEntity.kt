package com.expenseapp.salarytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.TreeState
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: GoalType,
    val targetAmountMinorUnits: Long = 0L,
    val targetDays: Int = 0,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String = "ACTIVE",
    val reachedMilestonesJson: String = "[]",
    val treeState: TreeState = TreeState.SEED,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = createdAtEpochMs,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val failureCount: Int = 0,
    val streakDays: Int = 0
)
