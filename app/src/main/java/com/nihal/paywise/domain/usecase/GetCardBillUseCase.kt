package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.util.CardCycleResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class CardBillUiModel(
    val billAmountPaise: Long,
    val paidAmountPaise: Long,
    val remainingToPayPaise: Long,
    val dueDate: LocalDate,
    val currentSpendPaise: Long // Spend since statement till now
)

class GetCardBillUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(account: Account): Flow<CardBillUiModel?> {
        if (account.type != AccountType.CARD || account.statementDay == null || account.dueDay == null) {
            return kotlinx.coroutines.flow.flowOf(null)
        }

        val today = LocalDate.now()
        val (start, end) = CardCycleResolver.getCycleRange(today, account.statementDay)
        
        // Due date for the statement generated at 'end' minus 1 day (approximately)
        val statementDate = end.atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(1)
        val dueDate = CardCycleResolver.getDueDate(statementDate, account.dueDay)

        return transactionRepository.getTransactionsBetweenStream(start, end).map { transactions ->
            val totalCharges = transactions
                .filter { it.accountId == account.id && it.type == TransactionType.EXPENSE }
                .sumOf { it.amountPaise }
            
            val totalPaid = transactions
                .filter { it.counterAccountId == account.id && it.type == TransactionType.TRANSFER }
                .sumOf { it.amountPaise }
            
            val remaining = (totalCharges - totalPaid).coerceAtLeast(0L)

            CardBillUiModel(
                billAmountPaise = totalCharges,
                paidAmountPaise = totalPaid,
                remainingToPayPaise = remaining,
                dueDate = dueDate,
                currentSpendPaise = totalCharges // For now same range, can be refined
            )
        }
    }
}
