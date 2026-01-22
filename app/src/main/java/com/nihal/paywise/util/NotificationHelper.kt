package com.nihal.paywise.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nihal.paywise.MainActivity
import com.nihal.paywise.R

object NotificationHelper {

    const val CHANNEL_ID = "recurring_reminders"
    const val CHANNEL_NAME = "Recurring Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for recurring transaction payments"

    const val BUDGET_CHANNEL_ID = "budget_alerts"
    const val BUDGET_CHANNEL_NAME = "Budget Alerts"
    private const val BUDGET_CHANNEL_DESCRIPTION = "Alerts for monthly budget thresholds"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Recurring Channel
            val recurringChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(recurringChannel)

            // Budget Channel
            val budgetChannel = NotificationChannel(BUDGET_CHANNEL_ID, BUDGET_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = BUDGET_CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(budgetChannel)
        }
    }

    fun showBudgetNotification(
        context: Context,
        threshold: Int,
        monthName: String
    ) {
        val title = "Budget alert"
        val content = if (threshold == 80) {
            "You've used 80% of your $monthName budget."
        } else {
            "You've crossed your $monthName budget."
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("nav_target", "budgets")
        }
        val openPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            ("BUDGET_$threshold".hashCode()),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            }
            notify("BUDGET_ALERT".hashCode() + threshold, builder.build())
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int, // Deprecated: we'll calculate it internally correctly
        title: String,
        content: String,
        recurringId: String,
        yearMonth: String,
        reminderType: String
    ) {
        // Fix B1: include yearMonth in notificationId to avoid collisions
        val finalNotificationId = "$recurringId|$yearMonth|$reminderType".hashCode()

        // 1. Content Intent (Open App)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            // Fix B4: Ensure deep link hit onNewIntent when app open
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("nav_target", "recurring")
            putExtra("recurringId", recurringId)
        }
        val openPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            getRequestCode(finalNotificationId, "OPEN"),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 2. Action: Mark Paid
        val paidIntent = Intent(context, RecurringReminderActionReceiver::class.java).apply {
            action = RecurringReminderActionReceiver.ACTION_MARK_PAID
            putExtra("RECURRING_ID", recurringId)
            putExtra("YEAR_MONTH", yearMonth)
            putExtra("NOTIFICATION_ID", finalNotificationId)
        }
        val paidPendingIntent = PendingIntent.getBroadcast(
            context,
            getRequestCode(finalNotificationId, "PAID"),
            paidIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. Actions: Snooze (1, 3, 7 days)
        val snooze1PendingIntent = createSnoozePendingIntent(context, recurringId, yearMonth, finalNotificationId, 1, "SNOOZE1")
        val snooze3PendingIntent = createSnoozePendingIntent(context, recurringId, yearMonth, finalNotificationId, 3, "SNOOZE3")
        val snooze7PendingIntent = createSnoozePendingIntent(context, recurringId, yearMonth, finalNotificationId, 7, "SNOOZE7")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Mark Paid", paidPendingIntent)
            .addAction(0, "Snooze 1D", snooze1PendingIntent)
            .addAction(0, "Snooze 3D", snooze3PendingIntent)
            // Note: Android typically shows up to 3 actions. Snooze 7D is registered but might be hidden in shade.
            .addAction(0, "Snooze 7D", snooze7PendingIntent)

        with(NotificationManagerCompat.from(context)) {
            // Fix B2: only require permission on API 33+
            if (Build.VERSION.SDK_INT >= 33) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            
            Log.d("NotificationHelper", "Showing notification $finalNotificationId for $recurringId (actions: PAID, SNOOZE 1/3/7)")
            notify(finalNotificationId, builder.build())
        }
    }

    private fun createSnoozePendingIntent(
        context: Context,
        recurringId: String,
        yearMonth: String,
        notificationId: Int,
        days: Int,
        tag: String
    ): PendingIntent {
        val action = when(days) {
            3 -> RecurringReminderActionReceiver.ACTION_SNOOZE_3
            7 -> RecurringReminderActionReceiver.ACTION_SNOOZE_7
            else -> RecurringReminderActionReceiver.ACTION_SNOOZE_1
        }
        val intent = Intent(context, RecurringReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra("RECURRING_ID", recurringId)
            putExtra("YEAR_MONTH", yearMonth)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            getRequestCode(notificationId, tag),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getRequestCode(notificationId: Int, action: String): Int {
        return (notificationId.toString() + action).hashCode()
    }
}
