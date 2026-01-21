package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nihal.paywise.domain.model.TransactionType
import java.time.Instant

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val amountPaise: Long,
    val timestamp: Instant,
    val type: TransactionType,
    val accountId: String,
    val counterAccountId: String?, // Only for transfer
    val categoryId: String?, // Nullable for transfer
    val note: String?,
    val recurringId: String?,
    val splitOfTransactionId: String?
)