package com.nihal.paywise.data.local.dao

import androidx.room.*
import com.nihal.paywise.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity)

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goals WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeActiveGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: String): SavingsGoalEntity?

    @Query("SELECT SUM(amountPaise) FROM transactions WHERE goalId = :goalId")
    fun observeTotalAllocated(goalId: String): Flow<Long?>
}