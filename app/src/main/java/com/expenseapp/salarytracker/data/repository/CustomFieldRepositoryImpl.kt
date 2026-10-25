package com.expenseapp.salarytracker.data.repository

import com.expenseapp.salarytracker.data.dao.CustomFieldDao
import com.expenseapp.salarytracker.data.entity.CustomFieldEntity
import com.expenseapp.salarytracker.data.mapper.toDomain
import com.expenseapp.salarytracker.data.mapper.toEntity
import com.expenseapp.salarytracker.domain.model.CustomField
import com.expenseapp.salarytracker.domain.model.CustomFieldType
import com.expenseapp.salarytracker.domain.repository.CustomFieldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomFieldRepositoryImpl(
    private val customFieldDao: CustomFieldDao
) : CustomFieldRepository {

    override fun getAllCustomFields(): Flow<List<CustomField>> {
        return customFieldDao.getAllCustomFields().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertCustomField(customField: CustomField): Long {
        return customFieldDao.insert(customField.toEntity())
    }

    override suspend fun deleteCustomField(id: Long) {
        customFieldDao.deleteById(id)
    }

    override suspend fun ensureDefaultCustomFields() {
        if (customFieldDao.getCount() == 0) {
            val defaults = listOf(
                CustomFieldEntity(
                    name = "Office/Personal",
                    type = CustomFieldType.CHOICE,
                    optionsJson = "[\"Personal\",\"Office / Work\"]"
                ),
                CustomFieldEntity(
                    name = "Transport Type",
                    type = CustomFieldType.CHOICE,
                    optionsJson = "[\"Rickshaw\",\"Bus\",\"CNG\",\"Metro / Train\",\"Ride Share\",\"Walking\"]"
                ),
                CustomFieldEntity(
                    name = "Location",
                    type = CustomFieldType.TEXT,
                    optionsJson = "[]"
                ),
                CustomFieldEntity(
                    name = "Payment Method",
                    type = CustomFieldType.CHOICE,
                    optionsJson = "[\"Cash\",\"bKash\",\"Nagad\",\"Debit Card\",\"Credit Card\",\"Bank Transfer\"]"
                ),
                CustomFieldEntity(
                    name = "Custom Tags",
                    type = CustomFieldType.TEXT,
                    optionsJson = "[]"
                )
            )
            customFieldDao.insertAll(defaults)
        }
    }
}
