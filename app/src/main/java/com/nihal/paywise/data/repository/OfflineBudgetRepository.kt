package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.BudgetDao
import com.nihal.paywise.data.local.entity.BudgetEntity
import com.nihal.paywise.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineBudgetRepository(private val budgetDao: BudgetDao) : BudgetRepository {
    override fun observeBudgetsForMonth(yearMonth: String): Flow<List<Budget>> {
        return budgetDao.observeBudgetsForMonth(yearMonth).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsertBudget(budget: Budget) {
        budgetDao.upsertBudget(budget.toEntity())
    }

    override suspend fun getBudgetForMonthCategory(yearMonth: String, categoryId: String?): Budget? {
        return budgetDao.getBudgetForMonthCategory(yearMonth, categoryId)?.toDomain()
    }

    override suspend fun deleteBudget(yearMonth: String, categoryId: String?) {
        budgetDao.deleteBudget(yearMonth, categoryId)
    }
}

private fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    yearMonth = yearMonth,
    categoryId = categoryId,
    amountPaise = amountPaise,
    updatedAt = updatedAt
)

private fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    yearMonth = yearMonth,
    categoryId = categoryId,
    amountPaise = amountPaise,
    updatedAt = updatedAt
)
