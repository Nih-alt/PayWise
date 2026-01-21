package com.nihal.paywise.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RecurringReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val recurringId = intent.getStringExtra("RECURRING_ID")
        val reminderType = intent.getStringExtra("REMINDER_TYPE")
        val yearMonth = intent.getStringExtra("YEAR_MONTH")
        val title = intent.getStringExtra("TITLE")
        val amountText = intent.getStringExtra("AMOUNT_TEXT")
        val dueText = intent.getStringExtra("DUE_TEXT")

        Log.d("RecurringReminder", "Received reminder for ID: $recurringId, Type: $reminderType")

        if (recurringId == null || reminderType == null || title == null || yearMonth == null) {
            Log.e("RecurringReminder", "Missing extras in intent")
            return
        }

        val notificationTitle = when (reminderType) {
            "LEAD" -> "Upcoming payment"
            "DUE" -> "Payment due today"
            "OVERDUE" -> "Payment overdue"
            "SNOOZED" -> "Payment reminder"
            else -> "Payment Reminder"
        }

        val content = if (reminderType == "SNOOZED") {
            "$title is still pending (snoozed)"
        } else {
            "$title • $amountText\n$dueText"
        }
        
        // Use a unique notification ID (e.g., hash of recurringId + type)
        val notificationId = (recurringId + reminderType).hashCode()

        NotificationHelper.showNotification(
            context, 
            notificationId, 
            notificationTitle, 
            content,
            recurringId,
            yearMonth,
            reminderType
        )
    }
}