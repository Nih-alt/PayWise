package com.nihal.paywise.di

import android.content.Context
import android.app.Application
import com.nihal.paywise.data.local.AppDatabase
import com.nihal.paywise.data.local.AppLockRepository
import com.nihal.paywise.data.local.UserPreferencesRepository
import com.nihal.paywise.data.repository.*
import com.nihal.paywise.domain.usecase.*
import com.nihal.paywise.domain.usecase.applock.*
import com.nihal.paywise.util.LockManager
import com.nihal.paywise.util.RecurringReminderScheduler

interface AppContainer {
    val accountRepository: AccountRepository
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val recurringRepository: RecurringRepository
    val recurringSkipRepository: RecurringSkipRepository
    val recurringSnoozeRepository: RecurringSnoozeRepository
    val budgetRepository: BudgetRepository
    val backupRepository: BackupRepository
    val savingsGoalRepository: SavingsGoalRepository
    val attachmentRepository: AttachmentRepository
    val claimRepository: ClaimRepository
    val userPreferencesRepository: UserPreferencesRepository
    val appLockRepository: AppLockRepository
    val recurringReminderScheduler: RecurringReminderScheduler
    val lockManager: LockManager
    
    val markRecurringAsPaidUseCase: MarkRecurringAsPaidUseCase
    val undoMarkRecurringAsPaidUseCase: UndoMarkRecurringAsPaidUseCase
    val skipRecurringForMonthUseCase: SkipRecurringForMonthUseCase
    val runRecurringAutoPostUseCase: RunRecurringAutoPostUseCase
    val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase
    val syncRecurringLastPostedMonthUseCase: SyncRecurringLastPostedMonthUseCase
    val getMonthlyExpenseTotalsUseCase: GetMonthlyExpenseTotalsUseCase
    val getBudgetStatusUseCase: GetBudgetStatusUseCase
    val upsertBudgetUseCase: UpsertBudgetUseCase
    val getCategoryBreakdownUseCase: GetCategoryBreakdownUseCase
    val getMonthlyTrendUseCase: GetMonthlyTrendUseCase
    val getFixedVsDiscretionaryUseCase: GetFixedVsDiscretionaryUseCase
    val getCardStatementUseCase: GetCardStatementUseCase
    val getCardBillUseCase: GetCardBillUseCase
    val addGoalAllocationUseCase: AddGoalAllocationUseCase

    // App Lock Use Cases
    val getAppLockSettingsUseCase: GetAppLockSettingsUseCase
    val setPinUseCase: SetPinUseCase
    val verifyPinUseCase: VerifyPinUseCase
    val setLockEnabledUseCase: SetLockEnabledUseCase
    val setBiometricEnabledUseCase: SetBiometricEnabledUseCase
    val setAutoLockMinutesUseCase: SetAutoLockMinutesUseCase
    val markUnlockedNowUseCase: MarkUnlockedNowUseCase
    val registerFailedAttemptUseCase: RegisterFailedAttemptUseCase
    val resetFailedAttemptsUseCase: ResetFailedAttemptsUseCase
    val setCooldownUntilUseCase: SetCooldownUntilUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val appLockRepository: AppLockRepository by lazy {
        AppLockRepository(context)
    }

    override val lockManager: LockManager by lazy {
        LockManager(appLockRepository, context.applicationContext as Application)
    }

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

    override val recurringSkipRepository: RecurringSkipRepository by lazy {
        OfflineRecurringSkipRepository(database.recurringSkipDao())
    }

    override val recurringSnoozeRepository: RecurringSnoozeRepository by lazy {
        OfflineRecurringSnoozeRepository(database.recurringSnoozeDao())
    }

    override val budgetRepository: BudgetRepository by lazy {
        OfflineBudgetRepository(database.budgetDao())
    }

    override val backupRepository: BackupRepository by lazy {
        OfflineBackupRepository(context, database.backupDao())
    }

    override val savingsGoalRepository: SavingsGoalRepository by lazy {
        OfflineSavingsGoalRepository(database.savingsGoalDao())
    }

    override val attachmentRepository: AttachmentRepository by lazy {
        OfflineAttachmentRepository(database.attachmentDao())
    }

    override val claimRepository: ClaimRepository by lazy {
        OfflineClaimRepository(database.claimDao())
    }

    override val recurringReminderScheduler: RecurringReminderScheduler by lazy {
        RecurringReminderScheduler(context, recurringSnoozeRepository)
    }

    override val markRecurringAsPaidUseCase: MarkRecurringAsPaidUseCase by lazy {
        MarkRecurringAsPaidUseCase(transactionRepository, recurringRepository)
    }

    override val undoMarkRecurringAsPaidUseCase: UndoMarkRecurringAsPaidUseCase by lazy {
        UndoMarkRecurringAsPaidUseCase(transactionRepository, recurringRepository)
    }

    override val skipRecurringForMonthUseCase: SkipRecurringForMonthUseCase by lazy {
        SkipRecurringForMonthUseCase(recurringSkipRepository)
    }

    override val runRecurringAutoPostUseCase: RunRecurringAutoPostUseCase by lazy {
        RunRecurringAutoPostUseCase(transactionRepository, recurringRepository, recurringSkipRepository)
    }

    override val syncRecurringLastPostedMonthUseCase: SyncRecurringLastPostedMonthUseCase by lazy {
        SyncRecurringLastPostedMonthUseCase(transactionRepository, recurringRepository)
    }

    override val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase by lazy {
        DeleteRecurringTransactionUseCase(transactionRepository, syncRecurringLastPostedMonthUseCase)
    }

    override val getMonthlyExpenseTotalsUseCase: GetMonthlyExpenseTotalsUseCase by lazy {
        GetMonthlyExpenseTotalsUseCase(transactionRepository)
    }

    override val getBudgetStatusUseCase: GetBudgetStatusUseCase by lazy {
        GetBudgetStatusUseCase(budgetRepository, getMonthlyExpenseTotalsUseCase)
    }

    override val upsertBudgetUseCase: UpsertBudgetUseCase by lazy {
        UpsertBudgetUseCase(budgetRepository)
    }

    override val getCategoryBreakdownUseCase: GetCategoryBreakdownUseCase by lazy {
        GetCategoryBreakdownUseCase(transactionRepository)
    }

    override val getMonthlyTrendUseCase: GetMonthlyTrendUseCase by lazy {
        GetMonthlyTrendUseCase(transactionRepository)
    }

    override val getFixedVsDiscretionaryUseCase: GetFixedVsDiscretionaryUseCase by lazy {
        GetFixedVsDiscretionaryUseCase(transactionRepository)
    }

    override val getCardStatementUseCase: GetCardStatementUseCase by lazy {
        GetCardStatementUseCase(transactionRepository)
    }

    override val getCardBillUseCase: GetCardBillUseCase by lazy {
        GetCardBillUseCase(transactionRepository)
    }

    override val addGoalAllocationUseCase: AddGoalAllocationUseCase by lazy {
        AddGoalAllocationUseCase(transactionRepository)
    }

    override val getAppLockSettingsUseCase: GetAppLockSettingsUseCase by lazy {
        GetAppLockSettingsUseCase(appLockRepository)
    }

    override val setPinUseCase: SetPinUseCase by lazy {
        SetPinUseCase(appLockRepository)
    }

    override val verifyPinUseCase: VerifyPinUseCase by lazy {
        VerifyPinUseCase(appLockRepository)
    }

    override val setLockEnabledUseCase: SetLockEnabledUseCase by lazy {
        SetLockEnabledUseCase(appLockRepository)
    }

    override val setBiometricEnabledUseCase: SetBiometricEnabledUseCase by lazy {
        SetBiometricEnabledUseCase(appLockRepository)
    }

    override val setAutoLockMinutesUseCase: SetAutoLockMinutesUseCase by lazy {
        SetAutoLockMinutesUseCase(appLockRepository)
    }

    override val markUnlockedNowUseCase: MarkUnlockedNowUseCase by lazy {
        MarkUnlockedNowUseCase(appLockRepository)
    }

    override val registerFailedAttemptUseCase: RegisterFailedAttemptUseCase by lazy {
        RegisterFailedAttemptUseCase(appLockRepository)
    }

    override val resetFailedAttemptsUseCase: ResetFailedAttemptsUseCase by lazy {
        ResetFailedAttemptsUseCase(appLockRepository)
    }

    override val setCooldownUntilUseCase: SetCooldownUntilUseCase by lazy {
        SetCooldownUntilUseCase(appLockRepository)
    }
}