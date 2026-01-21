package com.nihal.paywise.di

import android.content.Context
import com.nihal.paywise.data.local.AppDatabase
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.OfflineAccountRepository
import com.nihal.paywise.data.repository.OfflineCategoryRepository
import com.nihal.paywise.data.repository.OfflineRecurringRepository
import com.nihal.paywise.data.repository.OfflineTransactionRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.usecase.MarkRecurringAsPaidUseCase
import com.nihal.paywise.domain.usecase.RecurringAutoPostUseCase
import com.nihal.paywise.domain.usecase.SkipRecurringForMonthUseCase
import com.nihal.paywise.domain.usecase.UndoMarkRecurringAsPaidUseCase
import com.nihal.paywise.domain.usecase.DeleteRecurringTransactionUseCase
import com.nihal.paywise.domain.usecase.SyncRecurringLastPostedMonthUseCase
import com.nihal.paywise.util.RecurringReminderScheduler

interface AppContainer {
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val recurringRepository: RecurringRepository
    val recurringReminderScheduler: RecurringReminderScheduler
    val markRecurringAsPaidUseCase: MarkRecurringAsPaidUseCase
    val undoMarkRecurringAsPaidUseCase: UndoMarkRecurringAsPaidUseCase
    val skipRecurringForMonthUseCase: SkipRecurringForMonthUseCase
    val recurringAutoPostUseCase: RecurringAutoPostUseCase
    val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase
    val syncRecurringLastPostedMonthUseCase: SyncRecurringLastPostedMonthUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }

    override val accountRepository: AccountRepository by lazy {
        OfflineAccountRepository(database.accountDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        OfflineCategoryRepository(database.categoryDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        OfflineTransactionRepository(database.transactionDao())
    }

    override val recurringRepository: RecurringRepository by lazy {
        OfflineRecurringRepository(database.recurringDao())
    }

    override val recurringReminderScheduler: RecurringReminderScheduler by lazy {
        RecurringReminderScheduler(context)
    }

    override val markRecurringAsPaidUseCase: MarkRecurringAsPaidUseCase by lazy {
        MarkRecurringAsPaidUseCase(transactionRepository, recurringRepository)
    }

    override val undoMarkRecurringAsPaidUseCase: UndoMarkRecurringAsPaidUseCase by lazy {
        UndoMarkRecurringAsPaidUseCase(transactionRepository, recurringRepository)
    }

    override val skipRecurringForMonthUseCase: SkipRecurringForMonthUseCase by lazy {
        SkipRecurringForMonthUseCase(recurringRepository)
    }

    override val recurringAutoPostUseCase: RecurringAutoPostUseCase by lazy {
        RecurringAutoPostUseCase(transactionRepository, recurringRepository)
    }

    override val syncRecurringLastPostedMonthUseCase: SyncRecurringLastPostedMonthUseCase by lazy {
        SyncRecurringLastPostedMonthUseCase(transactionRepository, recurringRepository)
    }

    override val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase by lazy {
        DeleteRecurringTransactionUseCase(transactionRepository, syncRecurringLastPostedMonthUseCase)
    }
}
