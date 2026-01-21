package com.nihal.paywise.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nihal.paywise.ExpenseTrackerApp
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
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
            val zoneId = ZoneId.systemDefault()
            val monthStart = currentMonth.atDay(1).atStartOfDay(zoneId).toInstant()
            val monthEndExclusive = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant()

            // 1. Fetch data needed for scheduling
            val activeRecurrings = container.recurringRepository.getActiveRecurring()
            val skippedEntities = container.recurringSkipRepository.getSkipsForYearMonth(currentMonthStr)
            val skippedIds = skippedEntities.map { it.recurringId }.toSet()
            val snoozeEntities = container.recurringSnoozeRepository.getForYearMonth(currentMonthStr)
            val snoozeMap = snoozeEntities.associate { it.recurringId to it.snoozedUntilEpochMillis }
            
            // Fetch paid IDs
            val transactions = container.transactionRepository.getTransactionsBetweenStream(monthStart, monthEndExclusive).first()
            val paidIds = transactions.mapNotNull { it.recurringId }.toSet()

            Log.d("RescheduleWorker", "Processing ${activeRecurrings.size} items. Skips: ${skippedIds.size}, Snoozes: ${snoozeMap.size}, Paid: ${paidIds.size}")

            // 2. Run Auto-Post
            container.runRecurringAutoPostUseCase()

            // 3. Reschedule Reminders
            container.recurringReminderScheduler.scheduleForYearMonth(
                yearMonth = currentMonth,
                recurrings = activeRecurrings,
                skippedIds = skippedIds,
                snoozes = snoozeMap,
                paidIds = paidIds
            )
            Log.d("RescheduleWorker", "Rescheduling complete")

            Result.success()
        } catch (e: Exception) {
            Log.e("RescheduleWorker", "Error in RescheduleRemindersWorker", e)
            Result.retry()
        }
    }
}