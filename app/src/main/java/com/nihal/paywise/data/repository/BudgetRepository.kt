package com.nihal.paywise.data.repository

import com.nihal.paywise.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeBudgetsForMonth(yearMonth: String): Flow<List<Budget>>
    suspend fun upsertBudget(budget: Budget)
    suspend fun getBudgetForMonthCategory(yearMonth: String, categoryId: String?): Budget?
    suspend fun deleteBudget(yearMonth: String, categoryId: String?)
}
