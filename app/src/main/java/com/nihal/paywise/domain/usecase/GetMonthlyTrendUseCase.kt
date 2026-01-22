package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.TransactionType
import com.nihal.paywise.domain.model.TrendItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class GetMonthlyTrendUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(anchorMonth: YearMonth): Flow<List<TrendItem>> {
        // Last 12 months including anchor
        val months = (0 until 12).map { anchorMonth.minusMonths(it.toLong()) }.reversed()
        val startInstant = months.first().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endInstant = anchorMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return transactionRepository.getTransactionsBetweenStream(startInstant, endInstant).map { transactions ->
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            
            months.map { month ->
                val monthStart = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                
                val monthTotal = expenses.filter { it.timestamp >= monthStart && it.timestamp < monthEnd }
                    .sumOf { it.amountPaise }
                
                TrendItem(
                    label = month.month.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                    amountPaise = monthTotal
                )
            }
        }
    }
}
