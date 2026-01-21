package com.nihal.paywise.domain.usecase

import android.util.Log
import com.nihal.paywise.data.repository.TransactionRepository

class DeleteRecurringTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val syncRecurringLastPostedMonthUseCase: SyncRecurringLastPostedMonthUseCase
) {
    private val TAG = "DeleteRecurringTxUseCase"

    /**
     * Deletes a transaction and recomputes the lastPostedYearMonth for the recurring item.
     */
    suspend fun execute(transactionId: String, recurringId: String) {
        Log.d(TAG, "Deleting transaction $transactionId for recurring $recurringId")
        
        // 1. Delete the specific transaction
        transactionRepository.deleteTransactionById(transactionId)
        
        // 2. Sync the recurring item's status
        syncRecurringLastPostedMonthUseCase.execute(recurringId)
    }
}