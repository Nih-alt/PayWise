package com.nihal.paywise.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nihal.paywise.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE timestamp >= :startInstant AND timestamp <= :endInstant ORDER BY timestamp DESC")
    fun observeBetween(startInstant: Instant, endInstant: Instant): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR counterAccountId = :accountId ORDER BY timestamp DESC")
    fun observeByAccount(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE recurringId IS NOT NULL ORDER BY timestamp DESC")
    fun observeRecurringTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId AND timestamp >= :start AND timestamp <= :end LIMIT 1")
    suspend fun getByRecurringAndRange(recurringId: String, start: Instant, end: Instant): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForRecurring(recurringId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId AND timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun observeTransactionsForRecurring(recurringId: String, start: Instant, end: Instant): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE recurringId = :recurringId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentTransactionsForRecurring(recurringId: String, limit: Int): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: String)
}