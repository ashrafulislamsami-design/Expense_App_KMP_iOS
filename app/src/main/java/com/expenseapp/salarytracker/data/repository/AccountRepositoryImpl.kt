package com.expenseapp.salarytracker.data.repository

import com.expenseapp.salarytracker.data.dao.AccountDao
import com.expenseapp.salarytracker.data.mapper.toDomain
import com.expenseapp.salarytracker.data.mapper.toEntity
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val dao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> =
        dao.getAllAccounts().map { entities -> entities.map { it.toDomain() } }

    override fun getActiveAccounts(): Flow<List<Account>> =
        dao.getActiveAccounts().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAccountById(id: Long): Account? =
        dao.getAccountById(id)?.toDomain()

    override suspend fun insertAccount(account: Account): Long =
        dao.insert(account.toEntity())

    override suspend fun updateAccount(account: Account) =
        dao.update(account.toEntity())

    override suspend fun archiveAccount(id: Long) =
        dao.archiveAccount(id)
}
