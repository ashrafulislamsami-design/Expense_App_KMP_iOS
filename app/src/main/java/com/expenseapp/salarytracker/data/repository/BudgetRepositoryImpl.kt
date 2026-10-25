package com.expenseapp.salarytracker.data.repository

import com.expenseapp.salarytracker.data.dao.BudgetDao
import com.expenseapp.salarytracker.data.mapper.toDomain
import com.expenseapp.salarytracker.data.mapper.toEntity
import com.expenseapp.salarytracker.domain.model.Budget
import com.expenseapp.salarytracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<Budget>> =
        dao.getAllBudgets().map { entities -> entities.map { it.toDomain() } }

    override fun getOverallBudget(): Flow<Budget?> =
        dao.getOverallBudget().map { it?.toDomain() }

    override suspend fun getBudgetById(id: Long): Budget? =
        dao.getBudgetById(id)?.toDomain()

    override suspend fun insertOrUpdateBudget(budget: Budget): Long =
        dao.insert(budget.toEntity())

    override suspend fun deleteBudget(id: Long) =
        dao.deleteById(id)
}
