package com.expenseapp.salarytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expenseapp.salarytracker.domain.model.CustomFieldType

@Entity(tableName = "custom_fields")
data class CustomFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CustomFieldType,
    val optionsJson: String = "[]",
    val isRequired: Boolean = false
)

@Entity(tableName = "user_settings")
data class UserSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
