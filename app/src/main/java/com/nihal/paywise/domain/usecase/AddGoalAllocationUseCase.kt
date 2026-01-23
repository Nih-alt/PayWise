package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.TransactionType
import java.time.Instant
import java.util.UUID

class AddGoalAllocationUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        goalId: String,
        fromAccountId: String,
        amountPaise: Long,
        note: String? = null
    ) {
        val allocationTxn = Transaction(
            id = UUID.randomUUID().toString(),
            amountPaise = amountPaise,
            timestamp = Instant.now(),
            type = TransactionType.TRANSFER,
            accountId = fromAccountId,
            counterAccountId = null, // Important: Null signals internal movement to Goal
            categoryId = null,
            note = note ?: "Goal Allocation",
            recurringId = null,
            splitOfTransactionId = null
        ).copy(goalId = goalId)
        
        // Note: The copy(goalId = ...) requires updated Transaction domain model.
        // We'll update the Repository to accept this.
        transactionRepository.insertTransaction(allocationTxn)
    }
}
