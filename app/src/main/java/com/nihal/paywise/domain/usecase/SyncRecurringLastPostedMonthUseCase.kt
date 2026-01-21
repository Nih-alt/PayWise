package com.nihal.paywise.domain.usecase

import android.util.Log
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import java.time.YearMonth
import java.time.ZoneId

class SyncRecurringLastPostedMonthUseCase(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository
) {
    private val TAG = "SyncRecurringMonth"

    /**
     * Recomputes and updates the lastPostedYearMonth for a specific recurring item
     * based on its latest transaction in the database.
     */
    suspend fun execute(recurringId: String) {
        val recurring = recurringRepository.getRecurringById(recurringId) ?: run {
            Log.d(TAG, "  Recurring item $recurringId not found")
            return
        }

        val latestTx = transactionRepository.getLatestTransactionForRecurring(recurringId)
        val newLastPostedYM = latestTx?.let {
            YearMonth.from(it.timestamp.atZone(ZoneId.systemDefault()))
        }

        recurringRepository.updateRecurring(
            recurring.copy(lastPostedYearMonth = newLastPostedYM?.toString())
        )
        Log.d(TAG, "  Synced $recurringId. New lastPostedYearMonth: $newLastPostedYM")
    }
}
