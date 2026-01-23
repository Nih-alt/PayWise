package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthlyExpenseTotals(
    val overallSpentPaise: Long,
    val spentByCategory: Map<String, Long>
)

class GetMonthlyExpenseTotalsUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(yearMonth: YearMonth): Flow<MonthlyExpenseTotals> {
        val startInstant = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return invoke(startInstant, endInstant)
    }

    operator fun invoke(start: Instant, end: Instant): Flow<MonthlyExpenseTotals> {
        return transactionRepository.getTransactionsBetweenStream(start, end).map { transactions ->
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            
            // Identifying parent transactions of splits to avoid double counting
            val splitParentIds = transactions.mapNotNull { it.splitOfTransactionId }.toSet()
            val finalExpenses = expenses.filter { it.id !in splitParentIds }

            val overall = finalExpenses.sumOf { it.amountPaise }
            val byCategory = finalExpenses
                .filter { it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { entry -> entry.value.sumOf { it.amountPaise } }

            MonthlyExpenseTotals(
                overallSpentPaise = overall,
                spentByCategory = byCategory
            )
        }
    }
}
