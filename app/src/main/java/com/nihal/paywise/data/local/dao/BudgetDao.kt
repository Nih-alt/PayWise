package com.nihal.paywise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nihal.paywise.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun observeBudgetsForMonth(yearMonth: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    suspend fun getBudgetForMonthCategory(yearMonth: String, categoryId: String?): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth AND categoryId = :categoryId")
    suspend fun deleteBudget(yearMonth: String, categoryId: String?)
}
