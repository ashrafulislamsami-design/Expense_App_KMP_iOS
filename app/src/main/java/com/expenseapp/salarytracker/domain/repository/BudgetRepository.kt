package com.expenseapp.salarytracker.domain.repository

import com.expenseapp.salarytracker.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<Budget>>
    fun getOverallBudget(): Flow<Budget?>
    suspend fun getBudgetById(id: Long): Budget?
    suspend fun insertOrUpdateBudget(budget: Budget): Long
    suspend fun deleteBudget(id: Long)
}
