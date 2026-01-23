package com.nihal.paywise.data.local.dao

import androidx.room.*
import com.nihal.paywise.data.local.entity.*

@Dao
interface BackupDao {

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRecurring(): List<RecurringEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM recurring_skips")
    suspend fun getAllSkips(): List<RecurringSkipEntity>

    @Query("SELECT * FROM recurring_snoozes")
    suspend fun getAllSnoozes(): List<RecurringSnoozeEntity>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllSavingsGoals(): List<SavingsGoalEntity>

    @Query("SELECT * FROM attachments")
    suspend fun getAllAttachments(): List<AttachmentEntity>

    @Query("SELECT * FROM reimbursement_claims")
    suspend fun getAllClaims(): List<ClaimEntity>

    @Query("SELECT * FROM claim_items")
    suspend fun getAllClaimItems(): List<ClaimItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(items: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(items: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(items: List<RecurringEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(items: List<BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkips(items: List<RecurringSkipEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnoozes(items: List<RecurringSnoozeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(items: List<SavingsGoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(items: List<AttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaims(items: List<ClaimEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaimItems(items: List<ClaimItemEntity>)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearRecurring()

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM recurring_skips")
    suspend fun clearSkips()

    @Query("DELETE FROM recurring_snoozes")
    suspend fun clearSnoozes()

    @Query("DELETE FROM savings_goals")
    suspend fun clearSavingsGoals()

    @Query("DELETE FROM attachments")
    suspend fun clearAttachments()

    @Query("DELETE FROM reimbursement_claims")
    suspend fun clearClaims()

    @Query("DELETE FROM claim_items")
    suspend fun clearClaimItems()

    @Transaction
    suspend fun clearAllTables() {
        clearClaimItems()
        clearClaims()
        clearAttachments()
        clearTransactions()
        clearSkips()
        clearSnoozes()
        clearSavingsGoals()
        clearRecurring()
        clearBudgets()
        clearCategories()
        clearAccounts()
    }
}