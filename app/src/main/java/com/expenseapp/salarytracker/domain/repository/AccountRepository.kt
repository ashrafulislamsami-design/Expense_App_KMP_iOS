package com.expenseapp.salarytracker.domain.repository

import com.expenseapp.salarytracker.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getActiveAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun archiveAccount(id: Long)
}
