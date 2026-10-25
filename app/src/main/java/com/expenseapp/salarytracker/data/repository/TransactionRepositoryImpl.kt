package com.expenseapp.salarytracker.data.repository

import com.expenseapp.salarytracker.data.dao.TransactionDao
import com.expenseapp.salarytracker.data.mapper.toDomain
import com.expenseapp.salarytracker.data.mapper.toEntity
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions().map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> =
        dao.getTransactionsBetween(startDate, endDate).map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>> =
        dao.getTransactionsForAccount(accountId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTransactionById(id: Long): Transaction? =
        dao.getTransactionById(id)?.toDomain()

    override suspend fun insertTransaction(transaction: Transaction): Long =
        dao.insert(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) =
        dao.update(transaction.toEntity())

    override suspend fun deleteTransaction(transaction: Transaction) =
        dao.delete(transaction.toEntity())

    override suspend fun deleteTransactionById(id: Long) =
        dao.deleteById(id)
}
