package com.expenseapp.salarytracker.domain.repository

import com.expenseapp.salarytracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>
    fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteTransactionById(id: Long)
}
