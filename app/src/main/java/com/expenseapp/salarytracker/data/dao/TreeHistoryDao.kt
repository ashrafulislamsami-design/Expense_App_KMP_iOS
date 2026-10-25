package com.expenseapp.salarytracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expenseapp.salarytracker.data.entity.TreeHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreeHistoryDao {

    @Query("SELECT * FROM tree_history ORDER BY timestampEpochMs DESC, id DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 30): Flow<List<TreeHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: TreeHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TreeHistoryEntity>): List<Long>

    @Query("DELETE FROM tree_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tree_history")
    suspend fun deleteAll()
}
