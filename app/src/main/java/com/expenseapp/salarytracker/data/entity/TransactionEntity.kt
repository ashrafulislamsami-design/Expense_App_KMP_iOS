package com.expenseapp.salarytracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expenseapp.salarytracker.domain.model.TransactionType
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["destinationAccountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["date"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMinorUnits: Long,
    val type: TransactionType,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val date: LocalDate,
    val time: LocalTime,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val recurringRuleId: Long? = null,
    val originalTransactionId: Long? = null,
    val customFieldsJson: String = "{}",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = createdAtEpochMs
)

