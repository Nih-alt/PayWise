package com.nihal.paywise.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihal.paywise.ExpenseTrackerApp
import com.nihal.paywise.ui.addtxn.AddTransactionViewModel
import com.nihal.paywise.ui.home.HomeViewModel
import com.nihal.paywise.ui.recurring.AddRecurringViewModel
import com.nihal.paywise.ui.recurring.RecurringHistoryViewModel
import com.nihal.paywise.ui.recurring.RecurringListViewModel
import com.nihal.paywise.ui.recurring.RecurringTransactionsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            HomeViewModel(
                inventoryApplication().container.transactionRepository,
                inventoryApplication().container.accountRepository,
                inventoryApplication().container.categoryRepository,
                inventoryApplication().container.recurringRepository,
                inventoryApplication().container.runRecurringAutoPostUseCase
            )
        }
        initializer {
            AddTransactionViewModel(
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
