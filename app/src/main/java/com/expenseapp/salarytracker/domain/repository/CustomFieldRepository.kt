package com.expenseapp.salarytracker.domain.repository

import com.expenseapp.salarytracker.domain.model.CustomField
import kotlinx.coroutines.flow.Flow

interface CustomFieldRepository {
    fun getAllCustomFields(): Flow<List<CustomField>>
    suspend fun insertCustomField(customField: CustomField): Long
    suspend fun deleteCustomField(id: Long)
    suspend fun ensureDefaultCustomFields()
}
