package com.expenseapp.salarytracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expenseapp.salarytracker.data.entity.GoalEntity
import com.expenseapp.salarytracker.domain.model.TreeState
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = 'ACTIVE' ORDER BY id DESC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE status = :status ORDER BY id DESC")
    fun getGoalsByStatus(status: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>): List<Long>

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("UPDATE goals SET status = :status, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE goals SET reachedMilestonesJson = :milestonesJson, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateMilestones(id: Long, milestonesJson: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE goals SET treeState = :treeState, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateTreeState(id: Long, treeState: TreeState, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
