package com.nihal.paywise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nihal.paywise.data.local.entity.RecurringSkipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringSkipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkip(skip: RecurringSkipEntity)

    @Query("DELETE FROM recurring_skips WHERE recurringId = :recurringId AND yearMonth = :yearMonth")
    suspend fun deleteSkip(recurringId: String, yearMonth: String)

    @Query("SELECT * FROM recurring_skips WHERE yearMonth = :yearMonth")
    fun observeSkipsForYearMonth(yearMonth: String): Flow<List<RecurringSkipEntity>>

    @Query("SELECT * FROM recurring_skips WHERE yearMonth = :yearMonth")
    suspend fun getSkipsForYearMonth(yearMonth: String): List<RecurringSkipEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM recurring_skips WHERE recurringId = :recurringId AND yearMonth = :yearMonth)")
    suspend fun isSkipped(recurringId: String, yearMonth: String): Boolean
}