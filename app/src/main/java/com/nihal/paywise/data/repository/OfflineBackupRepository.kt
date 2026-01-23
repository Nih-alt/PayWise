package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.BackupDao
import com.nihal.paywise.data.local.entity.*
import com.nihal.paywise.domain.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class OfflineBackupRepository(
    private val backupDao: BackupDao
) : BackupRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun getFullBackupJson(): String {
        val backup = PayWiseBackup(
            exportedAtEpochMillis = System.currentTimeMillis(),
            accounts = backupDao.getAllAccounts().map { it.toBackup() },
            categories = backupDao.getAllCategories().map { it.toBackup() },
            transactions = backupDao.getAllTransactions().map { it.toBackup() },
            recurring = backupDao.getAllRecurring().map { it.toBackup() },
            budgets = backupDao.getAllBudgets().map { it.toBackup() },
            recurringSkips = backupDao.getAllSkips().map { it.toBackup() },
            recurringSnoozes = backupDao.getAllSnoozes().map { it.toBackup() }
        )
        return json.encodeToString(backup)
    }

    override suspend fun restoreFromJson(jsonString: String) {
        val backup = json.decodeFromString<PayWiseBackup>(jsonString)
        if (backup.version != 1) throw IllegalArgumentException("Unsupported backup version")

        // Perform in order to respect dependencies (though SQLite allows deferred)
        backupDao.clearAllTables()

        backupDao.insertAccounts(backup.accounts.map { it.toEntity() })
        backupDao.insertCategories(backup.categories.map { it.toEntity() })
        backupDao.insertRecurring(backup.recurring.map { it.toEntity() })
        backupDao.insertBudgets(backup.budgets.map { it.toEntity() })
        backupDao.insertTransactions(backup.transactions.map { it.toEntity() })
        backupDao.insertSkips(backup.recurringSkips.map { it.toEntity() })
        backupDao.insertSnoozes(backup.recurringSnoozes.map { it.toEntity() })
    }

    override suspend fun getTransactionsCsv(): String {
        val transactions = backupDao.getAllTransactions()
        val accounts = backupDao.getAllAccounts().associateBy { it.id }
        val categories = backupDao.getAllCategories().associateBy { it.id }

        val sb = StringBuilder()
        sb.append("Date,Type,Amount,Category,Account,Note,RecurringId\n")

        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

        transactions.sortedByDescending { it.timestamp }.forEach { tx ->
            val date = dtf.format(tx.timestamp)
            val type = tx.type.name
            val amount = (tx.amountPaise / 100.0).toString()
            val catName = categories[tx.categoryId]?.name ?: "Uncategorized"
            val accName = accounts[tx.accountId]?.name ?: "Unknown"
            val note = tx.note?.replace(", ", " ") ?: ""
            val recId = tx.recurringId ?: ""

            sb.append("$date,$type,$amount,$catName,$accName,$note,$recId\n")
        }

        return sb.toString()
    }
}

// Mappers
private fun AccountEntity.toBackup() = AccountBackup(id, name, type.name, openingBalancePaise)
private fun AccountBackup.toEntity() = AccountEntity(id, name, AccountType.valueOf(type), openingBalancePaise)

private fun CategoryEntity.toBackup() = CategoryBackup(id, name, color, kind.name, spendingGroup.name, parentId)
private fun CategoryBackup.toEntity() = CategoryEntity(id, name, color, CategoryKind.valueOf(kind), SpendingGroup.valueOf(spendingGroup), parentId)

private fun TransactionEntity.toBackup() = TransactionBackup(id, amountPaise, timestamp, type.name, accountId, counterAccountId, categoryId, note, recurringId, splitOfTransactionId)
private fun TransactionBackup.toEntity() = TransactionEntity(id, amountPaise, timestamp, TransactionType.valueOf(type), accountId, counterAccountId, categoryId, note, recurringId, splitOfTransactionId)

private fun RecurringEntity.toBackup() = RecurringBackup(id, title, amountPaise, accountId, categoryId, dueDay, leadDays, autoPost, skipIfPaid, startYearMonth, endYearMonth, lastPostedYearMonth, status.name)
private fun RecurringBackup.toEntity() = RecurringEntity(id, title, amountPaise, accountId, categoryId, dueDay, leadDays, autoPost, skipIfPaid, startYearMonth, endYearMonth, lastPostedYearMonth, RecurringStatus.valueOf(status))

private fun BudgetEntity.toBackup() = BudgetBackup(id, yearMonth, categoryId, amountPaise, updatedAt)
private fun BudgetBackup.toEntity() = BudgetEntity(id, yearMonth, categoryId, amountPaise, updatedAt)

private fun RecurringSkipEntity.toBackup() = RecurringSkipBackup(id, recurringId, yearMonth)
private fun RecurringSkipBackup.toEntity() = RecurringSkipEntity(id, recurringId, yearMonth)

private fun RecurringSnoozeEntity.toBackup() = RecurringSnoozeBackup(id, recurringId, yearMonth, snoozedUntilEpochMillis)
private fun RecurringSnoozeBackup.toEntity() = RecurringSnoozeEntity(id, recurringId, yearMonth, snoozedUntilEpochMillis)
