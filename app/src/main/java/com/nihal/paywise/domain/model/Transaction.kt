package com.nihal.paywise.domain.model

import com.nihal.paywise.data.local.entity.TransactionEntity
import java.time.Instant

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

data class Transaction(
    val id: String,
    val amountPaise: Long,
    val timestamp: Instant,
    val type: TransactionType,
    val accountId: String,
    val counterAccountId: String?,
        val categoryId: String?,
        val note: String?,
        val recurringId: String?,
        val splitOfTransactionId: String?,
        val goalId: String? = null
    )
    
    fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
        id = id,
        amountPaise = amountPaise,
        timestamp = timestamp,
        type = type,
        accountId = accountId,
        counterAccountId = counterAccountId,
        categoryId = categoryId,
        note = note,
        recurringId = recurringId,
        splitOfTransactionId = splitOfTransactionId,
        goalId = goalId
    )
    
    fun TransactionEntity.toDomain(): Transaction = Transaction(
        id = id,
        amountPaise = amountPaise,
        timestamp = timestamp,
        type = type,
        accountId = accountId,
        counterAccountId = counterAccountId,
        categoryId = categoryId,
        note = note,
        recurringId = recurringId,
        splitOfTransactionId = splitOfTransactionId,
        goalId = goalId
    )
    