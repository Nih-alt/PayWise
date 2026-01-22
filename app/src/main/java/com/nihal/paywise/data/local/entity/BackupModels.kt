package com.nihal.paywise.data.local.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilli())
    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}

@Serializable
data class PayWiseBackup(
    val version: Int = 1,
    val exportedAtEpochMillis: Long,
    val accounts: List<AccountBackup>,
    val categories: List<CategoryBackup>,
    val transactions: List<TransactionBackup>,
    val recurring: List<RecurringBackup>,
    val budgets: List<BudgetBackup>,
    val recurringSkips: List<RecurringSkipBackup>,
    val recurringSnoozes: List<RecurringSnoozeBackup>
)

@Serializable
data class AccountBackup(
    val id: String,
    val name: String,
    val type: String,
    val openingBalancePaise: Long
)

@Serializable
data class CategoryBackup(
    val id: String,
    val name: String,
    val color: Long,
    val kind: String,
    val parentId: String?
)

@Serializable
data class TransactionBackup(
    val id: String,
    val amountPaise: Long,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant,
    val type: String,
    val accountId: String,
    val counterAccountId: String?,
    val categoryId: String?,
    val note: String?,
    val recurringId: String?,
    val splitOfTransactionId: String?
)

@Serializable
data class RecurringBackup(
    val id: String,
    val title: String,
    val amountPaise: Long,
    val accountId: String,
    val categoryId: String,
    val dueDay: Int,
    val leadDays: Int,
    val autoPost: Boolean,
    val skipIfPaid: Boolean,
    val startYearMonth: String,
    val endYearMonth: String?,
    val lastPostedYearMonth: String?,
    val status: String
)

@Serializable
data class BudgetBackup(
    val id: String,
    val yearMonth: String,
    val categoryId: String?,
    val amountPaise: Long,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant
)

@Serializable
data class RecurringSkipBackup(
    val id: String,
    val recurringId: String,
    val yearMonth: String
)

@Serializable
data class RecurringSnoozeBackup(
    val id: String,
    val recurringId: String,
    val yearMonth: String,
    val snoozedUntilEpochMillis: Long
)
