package com.nihal.paywise.util

import android.util.Log
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nihal.paywise.domain.model.Recurring
import com.nihal.paywise.domain.model.RecurringStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

import com.nihal.paywise.data.repository.RecurringSnoozeRepository
import java.time.LocalDateTime
import java.time.LocalTime

class RecurringReminderScheduler(
    private val context: Context,
    private val snoozeRepository: RecurringSnoozeRepository
) {

    private val TAG = "ReminderScheduler"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    enum class ReminderType {
        LEAD, DUE, OVERDUE, SNOOZED
    }

    /**
     * Schedules reminders for a list of recurring transactions for a given month,
     * respecting skipped IDs, active status, and snoozes.
     */
    fun scheduleForYearMonth(
        yearMonth: YearMonth,
        recurrings: List<Recurring>,
        skippedIds: Set<String>,
        snoozes: Map<String, Long> = emptyMap()
    ) {
        Log.d(TAG, "Scheduling batch for $yearMonth. Count: ${recurrings.size}, Skipped: ${skippedIds.size}, Snoozes: ${snoozes.size}")
        
        for (recurring in recurrings) {
            // 1. Check status
            if (recurring.status != RecurringStatus.ACTIVE) {
                cancelForYearMonth(yearMonth, recurring.id)
                continue
            }

            // 2. Check skipped
            if (skippedIds.contains(recurring.id)) {
                Log.d(TAG, "  Skipping ${recurring.title} (User skipped)")
                cancelForYearMonth(yearMonth, recurring.id)
                continue
            }

            // 3. Schedule
            scheduleReminders(recurring, yearMonth, snoozes[recurring.id])
        }
    }

    /**
     * Schedules reminders for a recurring transaction in the given month,
     * handling snooze overrides.
     */
    fun scheduleReminders(recurring: Recurring, month: YearMonth, snoozedUntil: Long? = null) {
        val now = System.currentTimeMillis()
        
        if (snoozedUntil != null && snoozedUntil > now) {
            Log.d(TAG, "  Snooze override active for ${recurring.title} until $snoozedUntil")
            // Cancel all standard reminders for this YM
            cancelForYearMonth(yearMonth = month, recurringId = recurring.id)
            
            // Schedule only the snooze reminder
            schedule(
                recurring = recurring,
                triggerAtMillis = snoozedUntil,
                type = ReminderType.SNOOZED,
                month = month,
                amountText = MoneyFormatter.formatPaise(recurring.amountPaise),
                dueText = "Snoozed"
            )
            return
        }

        val dueDate = RecurringDateResolver.resolve(month, recurring.dueDay)
        val formattedAmount = MoneyFormatter.formatPaise(recurring.amountPaise)
        val formattedDueDate = DateTimeFormatterUtil.formatDate(dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        
        // 1. Lead reminder
        val leadDate = dueDate.minusDays(recurring.leadDays.toLong())
        schedule(recurring, leadDate, ReminderType.LEAD, month, formattedAmount, formattedDueDate)
        
        // 2. Due day reminder
        schedule(recurring, dueDate, ReminderType.DUE, month, formattedAmount, formattedDueDate)
        
        // 3. Overdue reminder
        val overdueDate = dueDate.plusDays(1)
        schedule(recurring, overdueDate, ReminderType.OVERDUE, month, formattedAmount, formattedDueDate)
    }

    private fun schedule(
        recurring: Recurring,
        date: LocalDate,
        type: ReminderType,
        month: YearMonth,
        amountText: String,
        dueText: String
    ) {
        val triggerAt = date.atTime(9, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
            
        schedule(recurring, triggerAt, type, month, amountText, dueText)
    }

    private fun schedule(
        recurring: Recurring,
        triggerAtMillis: Long,
        type: ReminderType,
        month: YearMonth,
        amountText: String,
        dueText: String
    ) {
        if (triggerAtMillis < System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, RecurringReminderReceiver::class.java).apply {
            action = "com.nihal.paywise.ACTION_RECURRING_REMINDER"
            putExtra("RECURRING_ID", recurring.id)
            putExtra("REMINDER_TYPE", type.name)
            putExtra("YEAR_MONTH", month.toString())
            putExtra("TITLE", recurring.title)
            putExtra("AMOUNT_TEXT", amountText)
            putExtra("DUE_TEXT", if (type == ReminderType.SNOOZED) "Pending" else "Due: $dueText")
        }

        val requestCode = getRequestCode(recurring.id, month.toString(), type)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        Log.d(TAG, "  Scheduled $type for ${recurring.title} at $triggerAtMillis")
    }

    /**
     * Cancels all scheduled reminders for a specific recurring transaction + YearMonth.
     */
    fun cancelForYearMonth(yearMonth: YearMonth, recurringId: String) {
        ReminderType.entries.forEach { type ->
            val intent = Intent(context, RecurringReminderReceiver::class.java).apply {
                action = "com.nihal.paywise.ACTION_RECURRING_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getRequestCode(recurringId, yearMonth.toString(), type),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it); it.cancel() }
        }
    }

    private fun getRequestCode(id: String, ymStr: String, type: ReminderType): Int {
        return (id + "|" + ymStr + "|" + type.name).hashCode()
    }
}
