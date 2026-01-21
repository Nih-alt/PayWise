package com.nihal.paywise.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nihal.paywise.MainActivity
import com.nihal.paywise.R

object NotificationHelper {

    const val CHANNEL_ID = "recurring_reminders"
    const val CHANNEL_NAME = "Recurring Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for recurring transaction payments"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT // or HIGH if urgency needed
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        recurringId: String,
        yearMonth: String,
        reminderType: String
    ) {
        // 1. Content Intent (Open App)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("nav_target", "recurring")
            putExtra("recurringId", recurringId)
        }
        val openPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            getRequestCode(notificationId, "OPEN"),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 2. Action: Mark Paid
        val paidIntent = Intent(context, RecurringReminderActionReceiver::class.java).apply {
            action = RecurringReminderActionReceiver.ACTION_MARK_PAID
            putExtra("RECURRING_ID", recurringId)
            putExtra("YEAR_MONTH", yearMonth)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val paidPendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(notificationId, "PAID"),
            paidIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. Action: Snooze 1 Day
        val snooze1Intent = Intent(context, RecurringReminderActionReceiver::class.java).apply {
            action = RecurringReminderActionReceiver.ACTION_SNOOZE_1
            putExtra("RECURRING_ID", recurringId)
            putExtra("YEAR_MONTH", yearMonth)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val snooze1PendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(notificationId, "SNOOZE1"),
            snooze1Intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Mark Paid", paidPendingIntent)
            .addAction(0, "Snooze 1 Day", snooze1PendingIntent)
            .addAction(0, "Open", openPendingIntent)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(notificationId, builder.build())
        }
    }

    private fun getRequestCode(notificationId: Int, action: String): Int {
        return (notificationId.toString() + action).hashCode()
    }
}
