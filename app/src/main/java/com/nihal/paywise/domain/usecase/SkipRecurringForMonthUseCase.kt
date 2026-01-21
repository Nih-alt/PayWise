package com.nihal.paywise.domain.usecase

import android.util.Log
import com.nihal.paywise.data.repository.RecurringRepository
import java.time.YearMonth

class SkipRecurringForMonthUseCase(
    private val recurringRepository: RecurringRepository
) {
    private val TAG = "SkipMonthUseCase"

    /**
     * Skips a recurring transaction for a specific month.
     * Updates lastPostedYearMonth without creating a transaction.
     */
    suspend fun execute(recurringId: String, month: YearMonth) {
        Log.d(TAG, "Skipping recurring $recurringId for month $month")
        val recurring = recurringRepository.getRecurringById(recurringId) ?: run {
            Log.d(TAG, "  Error: Recurring item not found")
            return
        }
        val currentMonthStr = month.toString()

        if (recurring.lastPostedYearMonth == currentMonthStr) {
            Log.d(TAG, "  Skip: Already handled for $month")
            return
        }

        recurringRepository.updateRecurring(
            recurring.copy(lastPostedYearMonth = currentMonthStr)
        )
        Log.d(TAG, "  Successfully skipped ${recurring.title} for $month.")
    }
}