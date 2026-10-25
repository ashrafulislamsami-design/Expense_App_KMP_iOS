package com.expenseapp.salarytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expenseapp.salarytracker.domain.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalanceMinorUnits: Long = 0L,
    val currency: String = "BDT",
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val colorHex: String = "#10B981"
)
