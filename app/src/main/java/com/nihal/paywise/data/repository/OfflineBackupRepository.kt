package com.nihal.paywise.data.repository

import android.content.Context
import com.nihal.paywise.data.local.dao.BackupDao
import com.nihal.paywise.data.local.entity.*
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.util.FileHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class OfflineBackupRepository(
    private val context: Context,
    private val backupDao: BackupDao
) : BackupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun getFullBackupJson(): String {
        val backup = createBackupModel()
        return json.encodeToString(backup)
    }

    private suspend fun createBackupModel(): PayWiseBackup {
        return PayWiseBackup(
            exportedAtEpochMillis = System.currentTimeMillis(),
            accounts = backupDao.getAllAccounts().map { it.toBackup() },
            categories = backupDao.getAllCategories().map { it.toBackup() },
            transactions = backupDao.getAllTransactions().map { it.toBackup() },
            recurring = backupDao.getAllRecurring().map { it.toBackup() },
            budgets = backupDao.getAllBudgets().map { it.toBackup() },
            recurringSkips = backupDao.getAllSkips().map { it.toBackup() },
            recurringSnoozes = backupDao.getAllSnoozes().map { it.toBackup() },
            goals = backupDao.getAllSavingsGoals().map { it.toBackup() },
            attachments = backupDao.getAllAttachments().map { it.toBackup() },
            claims = backupDao.getAllClaims().map { it.toBackup() },
            claimItems = backupDao.getAllClaimItems().map { it.toBackup() }
        )
    }

    suspend fun exportZip(outZipFile: File) {
        val backupDir = File(context.cacheDir, "backup_temp")
        backupDir.deleteRecursively()
        backupDir.mkdirs()

        val backupModel = createBackupModel()
        val jsonFile = File(backupDir, "backup.json")
        jsonFile.writeText(json.encodeToString(backupModel))

        val attachmentsSource = File(context.filesDir, "attachments")
        if (attachmentsSource.exists()) {
            val attachmentsTarget = File(backupDir, "attachments")
            attachmentsSource.copyRecursively(attachmentsTarget)
        }

        FileHelper.zipDirectory(backupDir, outZipFile)
        backupDir.deleteRecursively()
    }

    suspend fun importZip(zipFile: File) {
        val restoreDir = File(context.cacheDir, "restore_temp")
        restoreDir.deleteRecursively()
        restoreDir.mkdirs()

        try {
            FileHelper.unzip(zipFile, restoreDir)

            val jsonFile = File(restoreDir, "backup.json")
            if (!jsonFile.exists()) throw IllegalArgumentException("Invalid backup: backup.json missing")
            
            val backup = json.decodeFromString<PayWiseBackup>(jsonFile.readText())
            
            backupDao.clearAllTables()
            backupDao.insertAccounts(backup.accounts.map { it.toEntity() })
            backupDao.insertCategories(backup.categories.map { it.toEntity() })
            backupDao.insertRecurring(backup.recurring.map { it.toEntity() })
            backupDao.insertBudgets(backup.budgets.map { it.toEntity() })
            backupDao.insertSavingsGoals(backup.goals.map { it.toEntity() })
            backupDao.insertClaims(backup.claims.map { it.toEntity() })
            backupDao.insertClaimItems(backup.claimItems.map { it.toEntity() })
            backupDao.insertTransactions(backup.transactions.map { it.toEntity() })
            backupDao.insertSkips(backup.recurringSkips.map { it.toEntity() })
            backupDao.insertSnoozes(backup.recurringSnoozes.map { it.toEntity() })
            backupDao.insertAttachments(backup.attachments.map { it.toEntity() })

            val attachmentsRestoreSource = File(restoreDir, "attachments")
            val attachmentsFinalTarget = File(context.filesDir, "attachments")
            attachmentsFinalTarget.deleteRecursively()
            if (attachmentsRestoreSource.exists()) {
                attachmentsRestoreSource.copyRecursively(attachmentsFinalTarget)
            }

        } finally {
            restoreDir.deleteRecursively()
        }
    }

    override suspend fun restoreFromJson(jsonString: String) {
        val backup = json.decodeFromString<PayWiseBackup>(jsonString)
        backupDao.clearAllTables()
        backupDao.insertAccounts(backup.accounts.map { it.toEntity() })
        backupDao.insertCategories(backup.categories.map { it.toEntity() })
        backupDao.insertRecurring(backup.recurring.map { it.toEntity() })
        backupDao.insertBudgets(backup.budgets.map { it.toEntity() })
        backupDao.insertSavingsGoals(backup.goals.map { it.toEntity() })
        backupDao.insertClaims(backup.claims.map { it.toEntity() })
        backupDao.insertClaimItems(backup.claimItems.map { it.toEntity() })
        backupDao.insertTransactions(backup.transactions.map { it.toEntity() })
        backupDao.insertSkips(backup.recurringSkips.map { it.toEntity() })
        backupDao.insertSnoozes(backup.recurringSnoozes.map { it.toEntity() })
        backupDao.insertAttachments(backup.attachments.map { it.toEntity() })
    }

    override suspend fun getTransactionsCsv(): String {
        val transactions = backupDao.getAllTransactions()
        val accounts = backupDao.getAllAccounts().associateBy { it.id }
        val categories = backupDao.getAllCategories().associateBy { it.id }

        val sb = StringBuilder()
        sb.append("Date,Type,Amount,Category,Account,Note,RecurringId,GoalId\n")

        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

        transactions.sortedByDescending { it.timestamp }.forEach { tx ->
            val date = dtf.format(tx.timestamp)
            val type = tx.type.name
            val amount = (tx.amountPaise / 100.0).toString()
            val catName = categories[tx.categoryId]?.name ?: "Uncategorized"
            val accName = accounts[tx.accountId]?.name ?: "Unknown"
            val note = tx.note?.replace(", ", " ") ?: ""
            val recId = tx.recurringId ?: ""
            val goalId = tx.goalId ?: ""

            sb.append("$date,$type,$amount,$catName,$accName,$note,$recId,$goalId\n")
        }

        return sb.toString()
    }
}

// Mappers
private fun AccountEntity.toBackup() = AccountBackup(id, name, type.name, openingBalancePaise, statementDay, dueDay, creditLimitPaise)
private fun AccountBackup.toEntity() = AccountEntity(id, name, AccountType.valueOf(type), openingBalancePaise, statementDay, dueDay, creditLimitPaise)

private fun CategoryEntity.toBackup() = CategoryBackup(id, name, color, kind.name, spendingGroup.name, parentId)
private fun CategoryBackup.toEntity() = CategoryEntity(id, name, color, CategoryKind.valueOf(kind), SpendingGroup.valueOf(spendingGroup), parentId)

private fun TransactionEntity.toBackup() = TransactionBackup(id, amountPaise, timestamp, type.name, accountId, counterAccountId, categoryId, note, recurringId, splitOfTransactionId, goalId)
private fun TransactionBackup.toEntity() = TransactionEntity(id, amountPaise, timestamp, TransactionType.valueOf(type), accountId, counterAccountId, categoryId, note, recurringId, splitOfTransactionId, goalId)

private fun RecurringEntity.toBackup() = RecurringBackup(id, title, amountPaise, accountId, categoryId, dueDay, leadDays, autoPost, skipIfPaid, startYearMonth, endYearMonth, lastPostedYearMonth, status.name)
private fun RecurringBackup.toEntity() = RecurringEntity(id, title, amountPaise, accountId, categoryId, dueDay, leadDays, autoPost, skipIfPaid, startYearMonth, endYearMonth, lastPostedYearMonth, RecurringStatus.valueOf(status))

private fun BudgetEntity.toBackup() = BudgetBackup(id, yearMonth, categoryId, amountPaise, updatedAt)
private fun BudgetBackup.toEntity() = BudgetEntity(id, yearMonth, categoryId, amountPaise, updatedAt)

private fun RecurringSkipEntity.toBackup() = RecurringSkipBackup(id, recurringId, yearMonth)
private fun RecurringSkipBackup.toEntity() = RecurringSkipEntity(id, recurringId, yearMonth)

private fun RecurringSnoozeEntity.toBackup() = RecurringSnoozeBackup(id, recurringId, yearMonth, snoozedUntilEpochMillis)
private fun RecurringSnoozeBackup.toEntity() = RecurringSnoozeEntity(id, recurringId, yearMonth, snoozedUntilEpochMillis)

private fun SavingsGoalEntity.toBackup() = SavingsGoalBackup(id, title, targetAmountPaise, targetDateEpochMillis, color, isArchived, createdAt)
private fun SavingsGoalBackup.toEntity() = SavingsGoalEntity(id, title, targetAmountPaise, targetDateEpochMillis, color, isArchived, createdAt)

private fun AttachmentEntity.toBackup() = AttachmentBackup(id, txnId, claimId, storedRelativePath, originalFileName, mimeType, byteSize, createdAt)
private fun AttachmentBackup.toEntity() = AttachmentEntity(id, txnId, claimId, storedRelativePath, originalFileName, mimeType, byteSize, createdAt)

private fun ClaimEntity.toBackup() = ClaimBackup(id, title, status.name, notes, createdAt, submittedAt, approvedAt, reimbursedAt, reimbursedAmountPaise, incomeTxnId)
private fun ClaimBackup.toEntity() = ClaimEntity(id, title, ClaimStatus.valueOf(status), notes, createdAt, submittedAt, approvedAt, reimbursedAt, reimbursedAmountPaise, incomeTxnId)

private fun ClaimItemEntity.toBackup() = ClaimItemBackup(claimId, txnId, includeAmountPaise)
private fun ClaimItemBackup.toEntity() = ClaimItemEntity(claimId, txnId, includeAmountPaise)