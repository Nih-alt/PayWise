package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.BudgetRepository
import com.nihal.paywise.domain.model.Budget
import java.time.Instant
import java.time.YearMonth

class UpsertBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(yearMonth: YearMonth, categoryId: String?, amountPaise: Long) {
        val yearMonthStr = yearMonth.toString()
        val id = if (categoryId == null) {
            "$yearMonthStr|overall"
        } else {
            "$yearMonthStr|cat|$categoryId"
        }

        val budget = Budget(
            id = id,
            yearMonth = yearMonthStr,
            categoryId = categoryId,
            amountPaise = amountPaise,
            updatedAt = Instant.now()
        )
        budgetRepository.upsertBudget(budget)
    }
}
