package com.expenseapp.salarytracker.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expenseapp.salarytracker.data.entity.CustomFieldEntity
import com.expenseapp.salarytracker.data.entity.RecurringRuleEntity
import com.expenseapp.salarytracker.data.entity.UserSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingDao {
    @Query("SELECT * FROM user_settings")
    fun getAllSettings(): Flow<List<UserSettingEntity>>

    @Query("SELECT value FROM user_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<String?>

    @Query("SELECT value FROM user_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingDirect(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: UserSettingEntity)
}

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isPaused = 0 ORDER BY nextDueDate ASC")
    fun getActiveRules(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): RecurringRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RecurringRuleEntity): Long

    @androidx.room.Update
    suspend fun update(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET isPaused = :isPaused WHERE id = :id")
    suspend fun setPaused(id: Long, isPaused: Boolean)

    @Query("UPDATE recurring_rules SET nextDueDate = :nextDueDate WHERE id = :id")
    suspend fun updateNextDueDate(id: Long, nextDueDate: java.time.LocalDate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RecurringRuleEntity>): List<Long>

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_rules")
    suspend fun deleteAll()
}

@Dao
interface CustomFieldDao {
    @Query("SELECT * FROM custom_fields ORDER BY id ASC")
    fun getAllCustomFields(): Flow<List<CustomFieldEntity>>

    @Query("SELECT COUNT(*) FROM custom_fields")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(field: CustomFieldEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fields: List<CustomFieldEntity>): List<Long>

    @Query("DELETE FROM custom_fields WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM custom_fields")
    suspend fun deleteAll()
}
