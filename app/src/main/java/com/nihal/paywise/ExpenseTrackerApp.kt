package com.nihal.paywise

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nihal.paywise.data.local.DatabaseSeeder
import com.nihal.paywise.di.AppContainer
import com.nihal.paywise.di.DefaultAppContainer
import com.nihal.paywise.util.NotificationHelper
import com.nihal.paywise.util.RecurringReminderScheduler
import com.nihal.paywise.util.RescheduleRemindersWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ExpenseTrackerApp : Application() {
    lateinit var container: AppContainer
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        NotificationHelper.createNotificationChannels(this)
        RecurringReminderScheduler(this, container.recurringSnoozeRepository).scheduleDailyCheck()
    }
}