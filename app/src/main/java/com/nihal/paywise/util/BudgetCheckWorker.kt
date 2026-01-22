package com.nihal.paywise.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nihal.paywise.ExpenseTrackerApp
import kotlinx.coroutines.flow.first
import java.time.YearMonth

class BudgetCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val appContainer = (applicationContext as ExpenseTrackerApp).container
        val getBudgetStatusUseCase = appContainer.getBudgetStatusUseCase
        val userPreferencesRepository = appContainer.userPreferencesRepository
        
        val currentMonth = YearMonth.now()
        val monthStr = currentMonth.toString()
        val budgetStatus = getBudgetStatusUseCase(currentMonth).first()
        val overall = budgetStatus.overall ?: return Result.success()

        val percent = overall.percentUsed
        val monthName = DateTimeFormatterUtil.formatYearMonth(currentMonth)

        // Check 100% Threshold
        if (percent >= 1.0f) {
            val alreadyAlerted = userPreferencesRepository.isBudgetAlertFired(monthStr, 100)
            if (!alreadyAlerted) {
                NotificationHelper.showBudgetNotification(applicationContext, 100, monthName)
                userPreferencesRepository.markBudgetAlertFired(monthStr, 100)
            }
        } 
        
        // Check 80% Threshold
        if (percent >= 0.8f) {
            val alreadyAlerted = userPreferencesRepository.isBudgetAlertFired(monthStr, 80)
            if (!alreadyAlerted) {
                NotificationHelper.showBudgetNotification(applicationContext, 80, monthName)
                userPreferencesRepository.markBudgetAlertFired(monthStr, 80)
            }
        }

        return Result.success()
    }
}
