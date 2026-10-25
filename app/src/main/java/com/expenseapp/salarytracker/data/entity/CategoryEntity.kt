package com.expenseapp.salarytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "category",
    val colorHex: String = "#3B82F6",
    val parentCategoryId: Long? = null,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)
