package com.nihal.paywise.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Received intent action: ${intent.action}")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Enqueuing RescheduleRemindersWorker")
            val workRequest = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}