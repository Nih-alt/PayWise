package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.TransactionDao
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.toDomain
import com.nihal.paywise.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class OfflineTransactionRepository(private val transactionDao: TransactionDao) : TransactionRepository {
    override fun getAllTransactionsStream(): Flow<List<Transaction>> = 
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun getRecurringTransactionsStream(): Flow<List<Transaction>> =
        transactionDao.observeRecurringTransactions().map { list -> list.map { it.toDomain() } }

    override fun getTransactionsBetweenStream(start: Instant, end: Instant): Flow<List<Transaction>> = 
        transactionDao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }
        
    override fun getTransactionsByAccountStream(accountId: String): Flow<List<Transaction>> = 
        transactionDao.observeByAccount(accountId).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsForRecurringInRangeStream(recurringId: String, start: Instant, end: Instant): Flow<List<Transaction>> =
        transactionDao.observeTransactionsForRecurring(recurringId, start, end).map { list -> list.map { it.toDomain() } }

    override suspend fun getTransactionById(id: String): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun hasTransactionForRecurringInRange(recurringId: String, start: Instant, end: Instant): Boolean {
        return transactionDao.getByRecurringAndRange(recurringId, start, end).isNotEmpty()
    }

    override suspend fun getLatestTransactionForRecurring(recurringId: String): Transaction? {
        return transactionDao.getLatestForRecurring(recurringId)?.toDomain()
    }
        
    override suspend fun insertTransaction(transaction: Transaction) = 
        transactionDao.insert(transaction.toEntity())
        
    override suspend fun deleteTransaction(transaction: Transaction) = 
        transactionDao.delete(transaction.toEntity())

    override suspend fun deleteTransactionById(transactionId: String) =
        transactionDao.deleteById(transactionId)
}