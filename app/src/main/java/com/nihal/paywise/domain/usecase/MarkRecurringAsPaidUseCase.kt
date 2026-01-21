package com.nihal.paywise.domain.usecase

import android.util.Log
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.TransactionType
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class MarkRecurringAsPaidUseCase(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository
) {
    private val TAG = "MarkAsPaidUseCase"

    /**
     * Manually marks a recurring transaction as paid for a specific month.
     * Creates a transaction and updates lastPostedYearMonth.
     * @return transactionId if successful, null otherwise.
     */
    suspend fun execute(recurringId: String, month: YearMonth): String? {
        Log.d(TAG, "Marking recurring $recurringId as paid for $month")
        val recurring = recurringRepository.getRecurringById(recurringId) ?: run {
            Log.d(TAG, "  Error: Recurring item not found")
            return null
        }
        val currentMonthStr = month.toString()

        // 1. Check logical flag
        if (recurring.lastPostedYearMonth == currentMonthStr) {
            Log.d(TAG, "  Skip: Already marked as posted in Recurring entity for $month")
            return null
        }

        // 2. Secondary idempotency: Check DB for actual transaction in this month range
        val monthStart = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val monthEnd = month.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
        
        if (transactionRepository.hasTransactionForRecurringInRange(recurring.id, monthStart, monthEnd)) {
            Log.d(TAG, "  Skip: Transaction already exists in DB for this month.")
            // Sync the flag if it was missing
            recurringRepository.updateRecurring(recurring.copy(lastPostedYearMonth = currentMonthStr))
            return null
        }

        val transactionId = UUID.randomUUID().toString()
        val transaction = Transaction(
            id = transactionId,
            amountPaise = recurring.amountPaise,
            timestamp = Instant.now(), // requirement: timestamp = now
            type = TransactionType.EXPENSE,
            accountId = recurring.accountId,
            counterAccountId = null,
            categoryId = recurring.categoryId,
            note = "Paid: ${recurring.title}", // requirement: "Paid: <title>"
            recurringId = recurring.id,
            splitOfTransactionId = null
        )

        transactionRepository.insertTransaction(transaction)
        recurringRepository.updateRecurring(
            recurring.copy(lastPostedYearMonth = currentMonthStr)
        )
        Log.d(TAG, "  Successfully marked ${recurring.title} as paid.")
        return transactionId
    }
}
