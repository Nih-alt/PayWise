package com.nihal.paywise.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihal.paywise.ExpenseTrackerApp
import com.nihal.paywise.ui.addtxn.TransactionEditorViewModel
import com.nihal.paywise.ui.budgets.BudgetsViewModel
import com.nihal.paywise.ui.home.HomeViewModel
import com.nihal.paywise.ui.lock.LockViewModel
import com.nihal.paywise.ui.onboarding.OnboardingViewModel
import com.nihal.paywise.ui.recurring.AddRecurringViewModel
import com.nihal.paywise.ui.recurring.RecurringHistoryViewModel
import com.nihal.paywise.ui.recurring.RecurringListViewModel
import com.nihal.paywise.ui.recurring.RecurringTransactionsViewModel
import com.nihal.paywise.ui.reports.ReportsViewModel
import com.nihal.paywise.ui.settings.SettingsViewModel
import com.nihal.paywise.ui.settings.setpin.SetPinViewModel
import com.nihal.paywise.ui.transactions.TransactionsListViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            SettingsViewModel(
                inventoryApplication().container.backupRepository,
                inventoryApplication().container.userPreferencesRepository,
                inventoryApplication().container.getAppLockSettingsUseCase,
                inventoryApplication().container.setLockEnabledUseCase,
                inventoryApplication().container.setPinUseCase,
                inventoryApplication().container.setBiometricEnabledUseCase,
                inventoryApplication().container.setAutoLockMinutesUseCase
            )
        }
        initializer {
            LockViewModel(
                inventoryApplication().container.appLockRepository,
                inventoryApplication().container.verifyPinUseCase,
                inventoryApplication().container.markUnlockedNowUseCase,
                inventoryApplication().container.registerFailedAttemptUseCase,
                inventoryApplication().container.resetFailedAttemptsUseCase,
                inventoryApplication().container.setCooldownUntilUseCase
            )
        }
        initializer {
            SetPinViewModel(
                inventoryApplication().container.setPinUseCase
            )
        }
        initializer {
            OnboardingViewModel(
                inventoryApplication().container.userPreferencesRepository
            )
        }
        initializer {
            BudgetsViewModel(
                inventoryApplication().container.budgetRepository,
                inventoryApplication().container.categoryRepository,
                inventoryApplication().container.getBudgetStatusUseCase,
                inventoryApplication().container.upsertBudgetUseCase
            )
        }
        initializer {
            HomeViewModel(
                inventoryApplication().container.transactionRepository,
                inventoryApplication().container.accountRepository,
                inventoryApplication().container.categoryRepository,
                inventoryApplication().container.recurringRepository,
                inventoryApplication().container.userPreferencesRepository,
                inventoryApplication().container.runRecurringAutoPostUseCase,
                inventoryApplication().container.getBudgetStatusUseCase
            )
        }
        initializer {
            TransactionsListViewModel(
                inventoryApplication().container.transactionRepository,
                inventoryApplication().container.accountRepository,
                inventoryApplication().container.categoryRepository
            )
        }
        initializer {
            TransactionEditorViewModel(
                inventoryApplication(),
                this.createSavedStateHandle(),
                inventoryApplication().container.transactionRepository,
                inventoryApplication().container.accountRepository,
                inventoryApplication().container.categoryRepository
            )
        }
        initializer {
            RecurringListViewModel(
                inventoryApplication().container.recurringRepository,
                inventoryApplication().container.transactionRepository,
                inventoryApplication().container.accountRepository,
                inventoryApplication().container.categoryRepository,
                inventoryApplication().container.recurringSkipRepository,
                inventoryApplication().container.recurringSnoozeRepository,
                inventoryApplication().container.recurringReminderScheduler,
                inventoryApplication().container.markRecurringAsPaidUseCase,
                inventoryApplication().container.undoMarkRecurringAsPaidUseCase,
                inventoryApplication().container.skipRecurringForMonthUseCase
            )
        }
        initializer {
            AddRecurringViewModel(
                inventoryApplication().container.recurringRepository,
                inventoryApplication().container.accountRepository,
                inventoryApplication().container.categoryRepository
            )
        }
        initializer {
            RecurringTransactionsViewModel(
                inventoryApplication().container.transactionRepository,
                inventoryApplication().container.recurringRepository,
                inventoryApplication().container.accountRepository,
                inventoryApplication().container.categoryRepository
            )
        }
        initializer {
            ReportsViewModel(
                inventoryApplication().container.getCategoryBreakdownUseCase,
                inventoryApplication().container.getMonthlyTrendUseCase,
                inventoryApplication().container.getFixedVsDiscretionaryUseCase,
                inventoryApplication().container.userPreferencesRepository
            )
        }
        initializer {
            RecurringHistoryViewModel(
                this.createSavedStateHandle(),
                inventoryApplication().container.recurringRepository,
                inventoryApplication().container.transactionRepository,
                inventoryApplication().container.accountRepository,
                inventoryApplication().container.categoryRepository,
                inventoryApplication().container.deleteRecurringTransactionUseCase,
                inventoryApplication().container.syncRecurringLastPostedMonthUseCase
            )
        }
    }
}

fun CreationExtras.inventoryApplication(): ExpenseTrackerApp =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApp)
