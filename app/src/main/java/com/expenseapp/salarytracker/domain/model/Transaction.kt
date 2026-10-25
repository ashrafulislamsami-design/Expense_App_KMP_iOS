package com.expenseapp.salarytracker.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Transaction(
    val id: Long = 0,
    val amount: MinorUnits,
    val type: TransactionType,
    val accountId: Long,
    val destinationAccountId: Long? = null, // Used for transfers
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val date: LocalDate,
    val time: LocalTime = LocalTime.now(),
    val note: String = "",
    val tags: List<String> = emptyList(),
    val recurringRuleId: Long? = null,
    val originalTransactionId: Long? = null, // Linked for refunds
    val customFieldValues: Map<String, String> = emptyMap(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = createdAtEpochMs
)

