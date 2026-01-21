package com.nihal.paywise.data.repository

import com.nihal.paywise.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccountsStream(): Flow<List<Account>>
    suspend fun getAccountCount(): Int
    suspend fun insertAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
}