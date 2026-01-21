package com.nihal.paywise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nihal.paywise.data.local.dao.AccountDao
import com.nihal.paywise.data.local.dao.CategoryDao
import com.nihal.paywise.data.local.dao.RecurringDao
import com.nihal.paywise.data.local.dao.TransactionDao
import com.nihal.paywise.data.local.entity.AccountEntity
import com.nihal.paywise.data.local.entity.CategoryEntity
import com.nihal.paywise.data.local.dao.RecurringSnoozeDao
import com.nihal.paywise.data.local.entity.RecurringSnoozeEntity
import com.nihal.paywise.data.local.dao.RecurringSkipDao
import com.nihal.paywise.data.local.entity.RecurringSkipEntity
import com.nihal.paywise.data.local.entity.RecurringEntity
import com.nihal.paywise.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurringEntity::class,
        RecurringSkipEntity::class,
        RecurringSnoozeEntity::class
    ],
    version = 4,
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

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "expense_tracker.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
