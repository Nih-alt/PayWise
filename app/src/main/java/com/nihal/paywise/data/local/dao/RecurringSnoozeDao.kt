package com.nihal.paywise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nihal.paywise.data.local.entity.RecurringSnoozeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringSnoozeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snooze: RecurringSnoozeEntity)

    @Query("DELETE FROM recurring_snoozes WHERE recurringId = :recurringId AND yearMonth = :yearMonth")
    suspend fun delete(recurringId: String, yearMonth: String)

    @Query("SELECT * FROM recurring_snoozes WHERE recurringId = :recurringId AND yearMonth = :yearMonth")
    suspend fun get(recurringId: String, yearMonth: String): RecurringSnoozeEntity?
    
    @Query("SELECT * FROM recurring_snoozes WHERE yearMonth = :yearMonth")
    fun observeForYearMonth(yearMonth: String): Flow<List<RecurringSnoozeEntity>>

    @Query("SELECT * FROM recurring_snoozes WHERE yearMonth = :yearMonth")
    suspend fun getForYearMonth(yearMonth: String): List<RecurringSnoozeEntity>
}
