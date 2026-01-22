package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.BudgetRepository
import com.nihal.paywise.domain.model.BudgetStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth

data class MonthlyBudgetStatus(
    val overall: BudgetStatus?,
    val byCategory: List<BudgetStatus>
)

class GetBudgetStatusUseCase(
    private val budgetRepository: BudgetRepository,
    private val getMonthlyExpenseTotalsUseCase: GetMonthlyExpenseTotalsUseCase
) {
    operator fun invoke(yearMonth: YearMonth): Flow<MonthlyBudgetStatus> {
        val yearMonthStr = yearMonth.toString() // "YYYY-MM"
        
        return combine(
            budgetRepository.observeBudgetsForMonth(yearMonthStr),
            getMonthlyExpenseTotalsUseCase(yearMonth)
        ) { budgets, totals ->
            val overallBudget = budgets.find { it.categoryId == null }
            val overallStatus = overallBudget?.let {
                BudgetStatus(
                    budgetPaise = it.amountPaise,
                    spentPaise = totals.overallSpentPaise,
                    categoryId = null
                )
            }

            val categoryStatuses = budgets.filter { it.categoryId != null }.map { budget ->
                BudgetStatus(
                    budgetPaise = budget.amountPaise,
                    spentPaise = totals.spentByCategory[budget.categoryId] ?: 0L,
                    categoryId = budget.categoryId
                )
            }

            MonthlyBudgetStatus(
                overall = overallStatus,
                byCategory = categoryStatuses
            )
        }
    }
}
