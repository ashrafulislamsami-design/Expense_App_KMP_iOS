package com.expenseapp.salarytracker.data.repository

import com.expenseapp.salarytracker.data.dao.CategoryDao
import com.expenseapp.salarytracker.data.mapper.toDomain
import com.expenseapp.salarytracker.data.mapper.toEntity
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        dao.getAllCategories().map { entities -> entities.map { it.toDomain() } }

    override fun getActiveCategories(): Flow<List<Category>> =
        dao.getActiveCategories().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getCategoryById(id: Long): Category? =
        dao.getCategoryById(id)?.toDomain()

    override suspend fun insertCategory(category: Category): Long =
        dao.insert(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        dao.update(category.toEntity())

    override suspend fun archiveCategory(id: Long) =
        dao.archiveCategory(id)
}
