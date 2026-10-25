package com.expenseapp.salarytracker.data.repository

import com.expenseapp.salarytracker.data.dao.GoalDao
import com.expenseapp.salarytracker.data.dao.TreeHistoryDao
import com.expenseapp.salarytracker.data.mapper.toDomain
import com.expenseapp.salarytracker.data.mapper.toEntity
import com.expenseapp.salarytracker.domain.model.Goal
import com.expenseapp.salarytracker.domain.model.GoalStatus
import com.expenseapp.salarytracker.domain.model.TreeHistory
import com.expenseapp.salarytracker.domain.model.TreeState
import com.expenseapp.salarytracker.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl(
    private val dao: GoalDao,
    private val treeHistoryDao: TreeHistoryDao
) : GoalRepository {

    override fun getAllGoals(): Flow<List<Goal>> =
        dao.getAllGoals().map { entities -> entities.map { it.toDomain() } }

    override fun getActiveGoals(): Flow<List<Goal>> =
        dao.getActiveGoals().map { entities -> entities.map { it.toDomain() } }

    override fun getGoalsByStatus(status: GoalStatus): Flow<List<Goal>> =
        dao.getGoalsByStatus(status.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getGoalById(id: Long): Goal? =
        dao.getGoalById(id)?.toDomain()

    override suspend fun insertGoal(goal: Goal): Long =
        dao.insert(goal.toEntity())

    override suspend fun updateGoal(goal: Goal) =
        dao.update(goal.toEntity())

    override suspend fun setGoalStatus(id: Long, status: GoalStatus) {
        dao.updateStatus(id, status.name)
    }

    override suspend fun updateMilestones(id: Long, milestones: List<Int>) {
        val json = "[" + milestones.joinToString(",") + "]"
        dao.updateMilestones(id, json)
    }

    override suspend fun updateTreeState(id: Long, treeState: TreeState) {
        dao.updateTreeState(id, treeState)
    }

    override suspend fun deleteGoal(id: Long) =
        dao.deleteById(id)

    override fun getTreeHistory(limit: Int): Flow<List<TreeHistory>> =
        treeHistoryDao.getRecentHistory(limit).map { entities -> entities.map { it.toDomain() } }

    override suspend fun recordTreeHistory(event: TreeHistory): Long =
        treeHistoryDao.insert(event.toEntity())
}
