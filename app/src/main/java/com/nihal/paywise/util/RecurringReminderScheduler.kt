package com.nihal.paywise.util

import android.util.Log
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nihal.paywise.domain.model.Recurring
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class RecurringReminderScheduler(private val context: Context) {

    private val TAG = "ReminderScheduler"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    enum class ReminderType {
        LEAD, DUE, OVERDUE
    }

    /**
     * Schedules three reminders for a recurring transaction in the given month:
     * 1. Lead reminder (dueDay - leadDays)
     * 2. Due day reminder
     * 3. Overdue reminder (dueDay + 1)
     */
    fun scheduleReminders(recurring: Recurring, month: YearMonth) {
        Log.d(TAG, "Scheduling reminders for ${recurring.title} in $month")
        val dueDate = RecurringDateResolver.resolve(month, recurring.dueDay)
        
        // 1. Lead reminder
        val leadDate = dueDate.minusDays(recurring.leadDays.toLong())
        schedule(recurring, leadDate, ReminderType.LEAD)
        
        // 2. Due day reminder
        schedule(recurring, dueDate, ReminderType.DUE)
        
        // 3. Overdue reminder
        val overdueDate = dueDate.plusDays(1)
        schedule(recurring, overdueDate, ReminderType.OVERDUE)
    }

    private fun schedule(recurring: Recurring, date: LocalDate, type: ReminderType) {
        // Only schedule if the date is today or in the future
        if (date.isBefore(LocalDate.now())) {
            Log.d(TAG, "  Skip $type: Date $date is in the past")
            return
        }

        val intent = Intent(context, RecurringReminderReceiver::class.java).apply {
            action = "com.nihal.paywise.ACTION_RECURRING_REMINDER"
            putExtra("RECURRING_ID", recurring.id)
            putExtra("REMINDER_TYPE", type.name)
        }

        val requestCode = getRequestCode(recurring.id, type)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set trigger at 9:00 AM local time
        val triggerAt = date.atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        Log.d(TAG, "  Scheduled $type for ${recurring.title} at $date 09:00 AM")
    }

    /**
     * Cancels all scheduled reminders for a specific recurring transaction.
     */
    fun cancelReminders(recurringId: String) {
        Log.d(TAG, "Canceling all reminders for recurring ID: $recurringId")
        ReminderType.entries.forEach { type ->
            val intent = Intent(context, RecurringReminderReceiver::class.java).apply {
                action = "com.nihal.paywise.ACTION_RECURRING_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getRequestCode(recurringId, type),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    private fun getRequestCode(id: String, type: ReminderType): Int {
        // Use hash code + type ordinal for a stable request code
        return id.hashCode() * 3 + type.ordinal
    }
}