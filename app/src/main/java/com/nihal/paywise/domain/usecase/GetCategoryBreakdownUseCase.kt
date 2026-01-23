package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.CategoryBreakdownRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class GetCategoryBreakdownUseCase(private val transactionRepository: TransactionRepository) {
    operator fun invoke(start: Instant, end: Instant): Flow<List<CategoryBreakdownRow>> {
        return transactionRepository.getCategoryBreakdownStream(start, end).map { list ->
            val total = list.sumOf { it.totalAmount }
            if (total == 0L) return@map list
            
            list.map { row ->
                row.copy(percentage = (row.totalAmount.toFloat() / total))
            }
        }
    }
}