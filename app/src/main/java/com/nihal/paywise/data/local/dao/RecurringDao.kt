package com.nihal.paywise.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nihal.paywise.data.local.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringEntity)

    @Update
    suspend fun update(recurring: RecurringEntity)

    @Delete
    suspend fun delete(recurring: RecurringEntity)

    @Query("SELECT * FROM recurring_transactions ORDER BY title ASC")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringEntity?

    @Query("SELECT * FROM recurring_transactions WHERE status = 'ACTIVE'")
    suspend fun getActive(): List<RecurringEntity>
}
