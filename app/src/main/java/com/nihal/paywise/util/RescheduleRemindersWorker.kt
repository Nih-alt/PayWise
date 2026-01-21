package com.nihal.paywise.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nihal.paywise.ExpenseTrackerApp
import java.time.YearMonth

class RescheduleRemindersWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RescheduleWorker", "Starting reminder rescheduling after boot")
        val app = applicationContext as ExpenseTrackerApp
        val container = app.container
        
        val activeRecurring = container.recurringRepository.getActiveRecurring()
        val scheduler = container.recurringReminderScheduler
        val currentMonth = YearMonth.now()

        Log.d("RescheduleWorker", "Found ${activeRecurring.size} active recurring items to reschedule")
        activeRecurring.forEach { recurring ->
            scheduler.scheduleReminders(recurring, currentMonth)
        }

        Log.d("RescheduleWorker", "Successfully finished rescheduling")
        return Result.success()
    }
}