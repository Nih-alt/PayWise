package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.CategoryReportItem
import com.nihal.paywise.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant

class GetCategoryBreakdownUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(start: Instant, end: Instant): Flow<List<CategoryReportItem>> {
        return combine(
            transactionRepository.getTransactionsBetweenStream(start, end),
            categoryRepository.getAllCategoriesStream()
        ) { transactions, categories ->
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            val totalExpense = expenses.sumOf { it.amountPaise }
            if (totalExpense == 0L) return@combine emptyList<CategoryReportItem>()

            val categoryMap = categories.associateBy { it.id }
            
            expenses.groupBy { it.categoryId }
                .map { (catId, items) ->
                    val catAmount = items.sumOf { it.amountPaise }
                    val category = categoryMap[catId]
                    CategoryReportItem(
                        categoryId = catId ?: "uncategorized",
                        categoryName = category?.name ?: "Uncategorized",
                        amountPaise = catAmount,
                        percentage = catAmount.toFloat() / totalExpense.toFloat()
                    )
                }
                .sortedByDescending { it.amountPaise }
        }
    }
}
