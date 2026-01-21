package com.nihal.paywise.domain.usecase

import android.util.Log
import com.nihal.paywise.data.repository.RecurringSkipRepository
import java.time.YearMonth

class SkipRecurringForMonthUseCase(
    private val recurringSkipRepository: RecurringSkipRepository
) {
    private val TAG = "SkipMonthUseCase"

    /**
     * Skips a recurring transaction for a specific month.
     */
    suspend fun execute(recurringId: String, month: YearMonth) {
        Log.d(TAG, "Skipping recurring $recurringId for month $month")
        recurringSkipRepository.insertSkip(recurringId, month.toString())
    }

    /**
     * Unskips a recurring transaction for a specific month.
     */
    suspend fun unskip(recurringId: String, month: YearMonth) {
        Log.d(TAG, "Unskipping recurring $recurringId for month $month")
        recurringSkipRepository.deleteSkip(recurringId, month.toString())
    }
}