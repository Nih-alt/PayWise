package com.nihal.paywise.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nihal.paywise.data.local.entity.TransactionEntity
import com.nihal.paywise.domain.model.CategoryBreakdownRow
import com.nihal.paywise.domain.model.SpendingGroupRow
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE timestamp >= :startInstant AND timestamp < :endInstant ORDER BY timestamp DESC")
    fun observeBetween(startInstant: Instant, endInstant: Instant): Flow<List<TransactionEntity>>

    @Query("""
        SELECT 
            c.id as categoryId, 
            c.name as categoryName, 
            c.color as categoryColor, 
            SUM(t.amountPaise) as totalAmount,
            0.0 as percentage
        FROM transactions t
        JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE' 
          AND t.timestamp >= :startInstant 
          AND t.timestamp < :endInstant
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategoryBreakdown(startInstant: Instant, endInstant: Instant): Flow<List<CategoryBreakdownRow>>

    @Query("""
        SELECT 
            c.spendingGroup as spendingGroup, 
            SUM(t.amountPaise) as totalAmount,
            0.0 as percentage
        FROM transactions t
        JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE' 
          AND t.timestamp >= :startInstant 
          AND t.timestamp < :endInstant
        GROUP BY c.spendingGroup
    """)
    fun getSpendingGroupStats(startInstant: Instant, endInstant: Instant): Flow<List<SpendingGroupRow>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR counterAccountId = :accountId ORDER BY timestamp DESC")
    fun observeByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE recurringId IS NOT NULL ORDER BY timestamp DESC")
    fun observeRecurringTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId AND timestamp >= :start AND timestamp < :end LIMIT 1")
    suspend fun getByRecurringAndRange(recurringId: String, start: Instant, end: Instant): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForRecurring(recurringId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId AND timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun observeTransactionsForRecurring(recurringId: String, start: Instant, end: Instant): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentTransactionsForRecurring(recurringId: String, limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE recurringId = :recurringId AND timestamp >= :start AND timestamp < :end)")
    suspend fun existsRecurringTransactionInYearMonth(recurringId: String, start: Instant, end: Instant): Boolean

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: String)
}