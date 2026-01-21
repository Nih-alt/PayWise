package com.nihal.paywise.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.nihal.paywise.ExpenseTrackerApp
import com.nihal.paywise.data.local.entity.RecurringSnoozeEntity
import com.nihal.paywise.domain.model.RecurringStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class RecurringReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_PAID = "com.nihal.paywise.ACTION_MARK_PAID"
        const val ACTION_SNOOZE_1 = "com.nihal.paywise.ACTION_SNOOZE_1"
        const val ACTION_SNOOZE_3 = "com.nihal.paywise.ACTION_SNOOZE_3"
        const val ACTION_SNOOZE_7 = "com.nihal.paywise.ACTION_SNOOZE_7"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val recurringId = intent.getStringExtra("RECURRING_ID")
        val yearMonthStr = intent.getStringExtra("YEAR_MONTH")
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)

        Log.d("ActionReceiver", "Action: $action, ID: $recurringId, YM: $yearMonthStr")

        if (recurringId == null || yearMonthStr == null) {
            Log.e("ActionReceiver", "Missing extras")
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        
        val app = context.applicationContext as ExpenseTrackerApp
        val container = app.container

        scope.launch {
            try {
                val ym = YearMonth.parse(yearMonthStr)
                when (action) {
                    ACTION_MARK_PAID -> {
                        val txnId = container.markRecurringAsPaidUseCase.execute(recurringId, ym)
                        if (txnId != null) {
                            Log.d("MarkPaidAction", "Created txn $txnId for $recurringId in $ym")
                        } else {
                            Log.d("MarkPaidAction", "Transaction already exists or failed for $recurringId in $ym")
                        }

                        // Cleanup current month
                        container.recurringSnoozeRepository.delete(recurringId, yearMonthStr)
                        container.recurringReminderScheduler.cancelForYearMonth(ym, recurringId)
                        NotificationManagerCompat.from(context).cancel(notificationId)

                        // Attempt to schedule next month
                        val recurring = container.recurringRepository.getRecurringById(recurringId)
                        if (recurring != null && recurring.status == RecurringStatus.ACTIVE) {
                            val nextMonth = ym.plusMonths(1)
                            val start = YearMonth.parse(recurring.startYearMonth)
                            val end = recurring.endYearMonth?.let { YearMonth.parse(it) }
                            
                            val isWithinRange = !nextMonth.isBefore(start) && (end == null || !nextMonth.isAfter(end))
                            
                            if (isWithinRange) {
                                val isSkipped = container.recurringSkipRepository.isSkipped(recurringId, nextMonth.toString())
                                if (!isSkipped) {
                                    container.recurringReminderScheduler.scheduleReminders(recurring, nextMonth)
                                    Log.d("MarkPaidAction", "Scheduled next month reminder for $nextMonth")
                                }
                            }
                        }
                    }
                    ACTION_SNOOZE_1, ACTION_SNOOZE_3, ACTION_SNOOZE_7 -> {
                        val days = when(action) {
                            ACTION_SNOOZE_1 -> 1
                            ACTION_SNOOZE_3 -> 3
                            ACTION_SNOOZE_7 -> 7
                            else -> 1
                        }
                        
                        val snoozeUntil = calculateSnoozeTime(days)
                        val snoozeEntity = RecurringSnoozeEntity(
                            id = "$recurringId|$yearMonthStr",
                            recurringId = recurringId,
                            yearMonth = yearMonthStr,
                            snoozedUntilEpochMillis = snoozeUntil
                        )
                        container.recurringSnoozeRepository.upsert(snoozeEntity)
                        
                        // Reschedule specific recurring
                        val recurring = container.recurringRepository.getRecurringById(recurringId)
                        if (recurring != null) {
                            container.recurringReminderScheduler.scheduleReminders(recurring, ym, snoozeUntil)
                        }
                        
                        Log.d("ActionReceiver", "Snoozed $recurringId until $snoozeUntil ($days days)")
                        NotificationManagerCompat.from(context).cancel(notificationId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ActionReceiver", "Error handling action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun calculateSnoozeTime(days: Int): Long {
        val now = LocalDateTime.now()
        var targetDate = now.plusDays(days.toLong()).toLocalDate()
        val targetTime = LocalTime.of(9, 0)
        
        var targetDateTime = LocalDateTime.of(targetDate, targetTime)
        
        // If the calculated time is somehow in the past (unlikely for days >= 1), move to next day
        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }
        
        return targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}