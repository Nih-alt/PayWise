package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.AccountDao
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.toDomain
import com.nihal.paywise.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineAccountRepository(private val accountDao: AccountDao) : AccountRepository {
    override fun getAllAccountsStream(): Flow<List<Account>> = 
        accountDao.observeAllAccounts().map { list -> list.map { it.toDomain() } }

    override fun getAccountStream(id: String): Flow<Account?> =
        accountDao.observeAccount(id).map { it?.toDomain() }

    override suspend fun getAccountCount(): Int = accountDao.getCount()

    override suspend fun insertAccount(account: Account) = 
        accountDao.insert(account.toEntity())

    override suspend fun updateAccount(account: Account) = 
        accountDao.update(account.toEntity())

    override suspend fun deleteAccount(account: Account) = 
        accountDao.delete(account.toEntity())
}