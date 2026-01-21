package com.nihal.paywise.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RecurringReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val recurringId = intent.getStringExtra("RECURRING_ID")
        val reminderType = intent.getStringExtra("REMINDER_TYPE")
        
        Log.d("RecurringReminder", "Received reminder for ID: $recurringId, Type: $reminderType")
        
        // actual notification creation logic will go here later
    }
}