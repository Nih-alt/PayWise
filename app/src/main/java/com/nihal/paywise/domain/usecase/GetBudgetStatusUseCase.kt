package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.BudgetRepository
import com.nihal.paywise.domain.model.BudgetStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthlyBudgetStatus(
    val overall: BudgetStatus?,
    val byCategory: List<BudgetStatus>
)

class GetBudgetStatusUseCase(
    private val budgetRepository: BudgetRepository,
    private val getMonthlyExpenseTotalsUseCase: GetMonthlyExpenseTotalsUseCase
) {
    operator fun invoke(yearMonth: YearMonth): Flow<MonthlyBudgetStatus> {
        val startInstant = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return invoke(yearMonth, startInstant, endInstant)
    }

    operator fun invoke(yearMonth: YearMonth, start: Instant, end: Instant): Flow<MonthlyBudgetStatus> {
        val yearMonthStr = yearMonth.toString() // "YYYY-MM"
        
        return combine(
            budgetRepository.observeBudgetsForMonth(yearMonthStr),
            getMonthlyExpenseTotalsUseCase(start, end)
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