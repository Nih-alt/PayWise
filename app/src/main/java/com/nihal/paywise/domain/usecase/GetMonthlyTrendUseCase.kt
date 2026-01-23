package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.MonthlyTrendRow
import com.nihal.paywise.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth

class GetMonthlyTrendUseCase(private val transactionRepository: TransactionRepository) {
    operator fun invoke(endInstant: Instant): Flow<List<MonthlyTrendRow>> {
        val zone = ZoneId.systemDefault()
        val endDateTime = endInstant.atZone(zone)
        val endYearMonth = YearMonth.from(endDateTime)
        val startYearMonth = endYearMonth.minusMonths(11)
        val startInstant = startYearMonth.atDay(1).atStartOfDay(zone).toInstant()

        return transactionRepository.getTransactionsBetweenStream(startInstant, endInstant).map { transactions ->
            val expenseMap = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { YearMonth.from(it.timestamp.atZone(zone)) }
                .mapValues { entry -> entry.value.sumOf { it.amountPaise } }

            val result = mutableListOf<MonthlyTrendRow>()
            var current = startYearMonth
            while (!current.isAfter(endYearMonth)) {
                result.add(
                    MonthlyTrendRow(
                        yearMonth = current,
                        totalAmount = expenseMap[current] ?: 0L
                    )
                )
                current = current.plusMonths(1)
            }
            result
        }
    }
}