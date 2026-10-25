package com.expenseapp.salarytracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
import java.time.LocalDate

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long? = null,
    val limitMinorUnits: Long,
    val period: BudgetPeriod,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val warningThresholdPercentage: Int = 80
)
