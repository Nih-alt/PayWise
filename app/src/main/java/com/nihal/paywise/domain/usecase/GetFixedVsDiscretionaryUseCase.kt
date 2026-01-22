package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.FixedVsDiscretionaryReport
import com.nihal.paywise.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant

class GetFixedVsDiscretionaryUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    // Configuration for Fixed categories
    private val FIXED_CATEGORY_KEYWORDS = listOf(
        "rent", "emi", "loan", "bill", "utility", "electricity", "water", "gas",
        "mobile", "recharge", "internet", "wifi", "broadband", "insurance",
        "subscription", "netflix", "prime", "maintenance", "society",
        "school", "college", "education", "tax"
    )

    operator fun invoke(start: Instant, end: Instant): Flow<FixedVsDiscretionaryReport> {
        return combine(
            transactionRepository.getTransactionsBetweenStream(start, end),
            categoryRepository.getAllCategoriesStream()
        ) { transactions, categories ->
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            val fixedCategoryIds = categories.filter { category ->
                val name = category.name.lowercase()
                FIXED_CATEGORY_KEYWORDS.any { keyword -> name.contains(keyword) }
            }.map { it.id }.toSet()

            var fixedTotal = 0L
            var discTotal = 0L

            expenses.forEach { tx ->
                if (tx.categoryId in fixedCategoryIds) {
                    fixedTotal += tx.amountPaise
                } else {
                    discTotal += tx.amountPaise
                }
            }

            FixedVsDiscretionaryReport(fixedTotal, discTotal)
        }
    }
}
