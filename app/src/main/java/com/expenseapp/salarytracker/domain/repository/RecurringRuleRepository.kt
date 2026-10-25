package com.expenseapp.salarytracker.domain.repository

import com.expenseapp.salarytracker.domain.model.RecurringRule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecurringRuleRepository {
    fun getAllRules(): Flow<List<RecurringRule>>
    fun getActiveRules(): Flow<List<RecurringRule>>
    suspend fun getRuleById(id: Long): RecurringRule?
    suspend fun insertOrUpdate(rule: RecurringRule): Long
    suspend fun setPaused(id: Long, isPaused: Boolean)
    suspend fun updateNextDueDate(id: Long, nextDueDate: LocalDate)
    suspend fun deleteRule(id: Long)
}
