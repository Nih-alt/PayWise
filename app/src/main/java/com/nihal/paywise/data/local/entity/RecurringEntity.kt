package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nihal.paywise.domain.model.RecurringStatus

@Entity(tableName = "recurring_transactions")
data class RecurringEntity(
    @PrimaryKey
    val id: String, // UUID
    val title: String,
    val amountPaise: Long,
    val accountId: String,
    val categoryId: String,
    val dueDay: Int, // 1..31, use -1 for LAST_DAY
    val leadDays: Int = 3,
    val autoPost: Boolean = true,
    val skipIfPaid: Boolean = true,
    val startYearMonth: String, // yyyy-MM
    val endYearMonth: String?, // nullable
    val lastPostedYearMonth: String?, // nullable
    val status: RecurringStatus
)
