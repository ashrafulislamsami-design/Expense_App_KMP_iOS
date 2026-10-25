package com.expenseapp.salarytracker.domain.repository

import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.TreeHistory
import com.expenseapp.salarytracker.domain.model.TreeState
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAllGoals(): Flow<List<Goal>>
    fun getActiveGoals(): Flow<List<Goal>>
    fun getGoalsByStatus(status: GoalStatus): Flow<List<Goal>>
    suspend fun getGoalById(id: Long): Goal?
    suspend fun insertGoal(goal: Goal): Long
    suspend fun updateGoal(goal: Goal)
    suspend fun setGoalStatus(id: Long, status: GoalStatus)
    suspend fun updateMilestones(id: Long, milestones: List<Int>)
    suspend fun updateTreeState(id: Long, treeState: TreeState)
    suspend fun deleteGoal(id: Long)

    // Tree History
    fun getTreeHistory(limit: Int = 30): Flow<List<TreeHistory>>
    suspend fun recordTreeHistory(event: TreeHistory): Long
}
