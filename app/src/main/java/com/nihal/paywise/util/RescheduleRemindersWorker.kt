package com.nihal.paywise.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nihal.paywise.ExpenseTrackerApp
import java.time.YearMonth
import java.time.ZoneId

class RescheduleRemindersWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RescheduleWorker", "Starting daily/boot work for recurring transactions")
        val app = applicationContext as ExpenseTrackerApp
        val container = app.container
        
        return try {
            val currentMonth = YearMonth.now(ZoneId.systemDefault())
            val currentMonthStr = currentMonth.toString()

            // 1. Fetch data needed for scheduling
            val activeRecurrings = container.recurringRepository.getActiveRecurring()
            val skippedEntities = container.recurringSkipRepository.getSkipsForYearMonth(currentMonthStr)
            val skippedIds = skippedEntities.map { it.recurringId }.toSet()
            val snoozeEntities = container.recurringSnoozeRepository.getForYearMonth(currentMonthStr)
            val snoozeMap = snoozeEntities.associate { it.recurringId to it.snoozedUntilEpochMillis }

            Log.d("RescheduleWorker", "Processing ${activeRecurrings.size} recurring items for $currentMonthStr")
            Log.d("RescheduleWorker", "Found ${skippedIds.size} skips and ${snoozeMap.size} active snoozes")

            // 2. Run Auto-Post (Check if any due today need posting)
            container.runRecurringAutoPostUseCase()
            Log.d("RescheduleWorker", "Auto-post check complete")

            // 3. Reschedule Reminders
            container.recurringReminderScheduler.scheduleForYearMonth(
                currentMonth,
                activeRecurrings,
                skippedIds,
                snoozeMap
            )
            Log.d("RescheduleWorker", "Rescheduling complete")

            Result.success()
        } catch (e: Exception) {
            Log.e("RescheduleWorker", "Error in RescheduleRemindersWorker", e)
            Result.retry()
        }
    }
}