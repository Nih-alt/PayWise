package com.nihal.paywise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nihal.paywise.data.local.dao.*
import com.nihal.paywise.data.local.entity.*

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurringEntity::class,
        RecurringSkipEntity::class,
        RecurringSnoozeEntity::class,
        BudgetEntity::class,
        SavingsGoalEntity::class,
        AttachmentEntity::class,
        ClaimEntity::class,
        ClaimItemEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(PayWiseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringDao(): RecurringDao
    abstract fun recurringSkipDao(): RecurringSkipDao
    abstract fun recurringSnoozeDao(): RecurringSnoozeDao
    abstract fun budgetDao(): BudgetDao
    abstract fun backupDao(): BackupDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun claimDao(): ClaimDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `amountPaise` INTEGER NOT NULL, 
                        `accountId` TEXT NOT NULL, 
                        `categoryId` TEXT NOT NULL, 
                        `dueDay` INTEGER NOT NULL, 
                        `leadDays` INTEGER NOT NULL DEFAULT 3, 
                        `autoPost` INTEGER NOT NULL DEFAULT 1, 
                        `skipIfPaid` INTEGER NOT NULL DEFAULT 1, 
                        `startYearMonth` TEXT NOT NULL, 
                        `endYearMonth` TEXT, 
                        `lastPostedYearMonth` TEXT, 
                        `status` TEXT NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_skips` (
                        `id` TEXT NOT NULL,
                        `recurringId` TEXT NOT NULL,
                        `yearMonth` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_snoozes` (
                        `id` TEXT NOT NULL,
                        `recurringId` TEXT NOT NULL,
                        `yearMonth` TEXT NOT NULL,
                        `snoozedUntilEpochMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` TEXT NOT NULL,
                        `yearMonth` TEXT NOT NULL,
                        `categoryId` TEXT,
                        `amountPaise` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_yearMonth` ON `budgets` (`yearMonth`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_yearMonth_categoryId` ON `budgets` (`yearMonth`, `categoryId`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `spendingGroup` TEXT NOT NULL DEFAULT 'DISCRETIONARY'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `statementDay` INTEGER")
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `dueDay` INTEGER")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `targetAmountPaise` INTEGER NOT NULL, 
                        `targetDateEpochMillis` INTEGER, 
                        `color` INTEGER NOT NULL, 
                        `isArchived` INTEGER NOT NULL DEFAULT 0, 
                        `createdAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `goalId` TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `accounts` ADD COLUMN `creditLimitPaise` INTEGER")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `attachments` (
                        `id` TEXT NOT NULL, 
                        `txnId` TEXT, 
                        `storedRelativePath` TEXT NOT NULL, 
                        `originalFileName` TEXT, 
                        `mimeType` TEXT NOT NULL, 
                        `byteSize` INTEGER NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`txnId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_txnId` ON `attachments` (`txnId`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reimbursement_claims` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `notes` TEXT, 
                        `createdAt` INTEGER NOT NULL, 
                        `submittedAt` INTEGER, 
                        `approvedAt` INTEGER, 
                        `reimbursedAt` INTEGER, 
                        `reimbursedAmountPaise` INTEGER, 
                        `incomeTxnId` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `claim_items` (
                        `claimId` TEXT NOT NULL, 
                        `txnId` TEXT NOT NULL, 
                        `includeAmountPaise` INTEGER NOT NULL, 
                        PRIMARY KEY(`claimId`, `txnId`), 
                        FOREIGN KEY(`claimId`) REFERENCES `reimbursement_claims`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`txnId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_claim_items_txnId` ON `claim_items` (`txnId`)")
                db.execSQL("ALTER TABLE `attachments` ADD COLUMN `claimId` TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "expense_tracker.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}