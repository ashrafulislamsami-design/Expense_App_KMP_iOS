package com.expenseapp.salarytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expenseapp.salarytracker.domain.model.TreeState

@Entity(tableName = "tree_history")
data class TreeHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val previousState: TreeState,
    val newState: TreeState,
    val reason: String,
    val contributingGoalId: Long? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
