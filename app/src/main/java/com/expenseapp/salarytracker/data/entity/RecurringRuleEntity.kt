package com.expenseapp.salarytracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expenseapp.salarytracker.domain.model.RecurrenceFrequency
import com.expenseapp.salarytracker.domain.model.TransactionType
import java.time.LocalDate

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amountMinorUnits: Long,
    val type: TransactionType,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    val categoryId: Long? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val intervalDays: Int = 30,
    val preferredDayOfMonth: Int? = null,
    val startDate: LocalDate,
    val nextDueDate: LocalDate,
    val endDate: LocalDate? = null,
    val autoPost: Boolean = false,
    val isPaused: Boolean = false,
    val note: String = ""
)
