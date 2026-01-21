package com.nihal.paywise.domain.usecase

import android.util.Log
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import java.time.YearMonth
import java.time.ZoneId

class UndoMarkRecurringAsPaidUseCase(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository
) {
    private val TAG = "UndoMarkAsPaidUseCase"

    /**
     * Undoes a mark as paid action by deleting the transaction and restoring the lastPostedYearMonth.
     */
    suspend fun execute(transactionId: String, recurringId: String) {
        Log.d(TAG, "Undoing payment for recurring $recurringId, transaction $transactionId")
        
        // 1. Delete the transaction
        val latestTxBeforeDeletion = transactionRepository.getLatestTransactionForRecurring(recurringId)
        if (latestTxBeforeDeletion?.id == transactionId) {
            transactionRepository.deleteTransaction(latestTxBeforeDeletion)
            Log.d(TAG, "  Transaction deleted successfully")
        } else {
            Log.d(TAG, "  Warning: Transaction to delete not found or not the latest")
            // Still try to delete if ID matches but ordering was weird (unlikely with timestamp)
            latestTxBeforeDeletion?.let { 
                if (it.id == transactionId) transactionRepository.deleteTransaction(it)
            }
        }

        // 2. Restore lastPostedYearMonth correctly
        val recurring = recurringRepository.getRecurringById(recurringId) ?: return
        val latestTxAfterDeletion = transactionRepository.getLatestTransactionForRecurring(recurringId)
        
        val restoredYM = latestTxAfterDeletion?.let {
            YearMonth.from(it.timestamp.atZone(ZoneId.systemDefault()))
        }

        recurringRepository.updateRecurring(
            recurring.copy(lastPostedYearMonth = restoredYM?.toString())
        )
        Log.d(TAG, "  Restored lastPostedYearMonth to: $restoredYM")
    }
}
