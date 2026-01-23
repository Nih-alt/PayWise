package com.nihal.paywise.data.repository

import android.content.Context
import com.nihal.paywise.data.local.dao.TransactionDao
import com.nihal.paywise.domain.model.CategoryBreakdownRow
import com.nihal.paywise.domain.model.SpendingGroupRow
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.toDomain
import com.nihal.paywise.domain.model.toEntity
import com.nihal.paywise.util.WidgetUpdateController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class OfflineTransactionRepository(
    private val context: Context,
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override fun getAllTransactionsStream(): Flow<List<Transaction>> = 
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun getRecurringTransactionsStream(): Flow<List<Transaction>> =
        transactionDao.observeRecurringTransactions().map { list -> list.map { it.toDomain() } }

    override fun getTransactionsBetweenStream(start: Instant, end: Instant): Flow<List<Transaction>> = 
        transactionDao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }

    override fun getCategoryBreakdownStream(start: Instant, end: Instant): Flow<List<CategoryBreakdownRow>> =
        transactionDao.getCategoryBreakdown(start, end)

    override fun getSpendingGroupStatsStream(start: Instant, end: Instant): Flow<List<SpendingGroupRow>> =
        transactionDao.getSpendingGroupStats(start, end)
        
    override fun getTransactionsByAccountStream(accountId: String): Flow<List<Transaction>> = 
        transactionDao.observeByAccount(accountId).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsForRecurringInRangeStream(recurringId: String, start: Instant, end: Instant): Flow<List<Transaction>> =
        transactionDao.observeTransactionsForRecurring(recurringId, start, end).map { list -> list.map { it.toDomain() } }

    override suspend fun getTransactionById(id: String): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun hasTransactionForRecurringInRange(recurringId: String, start: Instant, end: Instant): Boolean {
        return transactionDao.getByRecurringAndRange(recurringId, start, end) != null
    }

    override suspend fun existsRecurringTransactionInYearMonth(recurringId: String, start: Instant, end: Instant): Boolean {
        return transactionDao.existsRecurringTransactionInYearMonth(recurringId, start, end)
    }

    override suspend fun getLatestTransactionForRecurring(recurringId: String): Transaction? {
        return transactionDao.getLatestForRecurring(recurringId)?.toDomain()
    }
        
    override suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
        WidgetUpdateController.updateAllWidgets(context)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
        WidgetUpdateController.updateAllWidgets(context)
    }
        
    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
        WidgetUpdateController.updateAllWidgets(context)
    }

    override suspend fun deleteTransactionById(transactionId: String) {
        transactionDao.deleteById(transactionId)
        WidgetUpdateController.updateAllWidgets(context)
    }
}
