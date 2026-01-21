package com.nihal.paywise.data.repository

import com.nihal.paywise.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TransactionRepository {
    fun getAllTransactionsStream(): Flow<List<Transaction>>
    fun getRecurringTransactionsStream(): Flow<List<Transaction>>
    fun getTransactionsBetweenStream(start: Instant, end: Instant): Flow<List<Transaction>>
    fun getTransactionsByAccountStream(accountId: String): Flow<List<Transaction>>
    fun getTransactionsForRecurringInRangeStream(recurringId: String, start: Instant, end: Instant): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun hasTransactionForRecurringInRange(recurringId: String, start: Instant, end: Instant): Boolean
    suspend fun getLatestTransactionForRecurring(recurringId: String): Transaction?
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteTransactionById(transactionId: String)
}