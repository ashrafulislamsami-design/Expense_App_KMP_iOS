package com.expenseapp.salarytracker.data.repository

import com.expenseapp.salarytracker.data.dao.RecurringRuleDao
import com.expenseapp.salarytracker.data.mapper.toDomain
import com.expenseapp.salarytracker.data.mapper.toEntity
import com.expenseapp.salarytracker.domain.model.RecurringRule
import com.expenseapp.salarytracker.domain.repository.RecurringRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RecurringRuleRepositoryImpl(
    private val recurringRuleDao: RecurringRuleDao
) : RecurringRuleRepository {

    override fun getAllRules(): Flow<List<RecurringRule>> {
        return recurringRuleDao.getAllRules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveRules(): Flow<List<RecurringRule>> {
        return recurringRuleDao.getActiveRules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRuleById(id: Long): RecurringRule? {
        return recurringRuleDao.getRuleById(id)?.toDomain()
    }

    override suspend fun insertOrUpdate(rule: RecurringRule): Long {
        return if (rule.id == 0L) {
            recurringRuleDao.insert(rule.toEntity())
        } else {
            recurringRuleDao.update(rule.toEntity())
            rule.id
        }
    }

    override suspend fun setPaused(id: Long, isPaused: Boolean) {
        recurringRuleDao.setPaused(id, isPaused)
    }

    override suspend fun updateNextDueDate(id: Long, nextDueDate: LocalDate) {
        recurringRuleDao.updateNextDueDate(id, nextDueDate)
    }

    override suspend fun deleteRule(id: Long) {
        recurringRuleDao.deleteById(id)
    }
}
