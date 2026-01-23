package com.nihal.paywise.data.local

import androidx.room.TypeConverter
import com.nihal.paywise.domain.model.AccountType
import com.nihal.paywise.domain.model.CategoryKind
import com.nihal.paywise.domain.model.RecurringStatus
import com.nihal.paywise.domain.model.SpendingGroup
import com.nihal.paywise.domain.model.TransactionType
import java.time.Instant

class PayWiseConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun fromAccountType(value: AccountType): String {
        return value.name
    }

    @TypeConverter
    fun toAccountType(value: String): AccountType {
        return AccountType.valueOf(value)
    }

    @TypeConverter
    fun fromCategoryKind(value: CategoryKind): String {
        return value.name
    }

    @TypeConverter
    fun toCategoryKind(value: String): CategoryKind {
        return CategoryKind.valueOf(value)
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromRecurringStatus(value: RecurringStatus): String {
        return value.name
    }

    @TypeConverter
    fun toRecurringStatus(value: String): RecurringStatus {
        return RecurringStatus.valueOf(value)
    }

    @TypeConverter
    fun fromSpendingGroup(value: SpendingGroup): String {
        return value.name
    }

    @TypeConverter
    fun toSpendingGroup(value: String): SpendingGroup {
        return SpendingGroup.valueOf(value)
    }
}