package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["yearMonth"]),
        Index(value = ["yearMonth", "categoryId"])
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String, // Format: "YYYY-MM|overall" or "YYYY-MM|cat|<categoryId>"
    val yearMonth: String, // "YYYY-MM"
    val categoryId: String?, // Null for overall budget
    val amountPaise: Long,
    val updatedAt: Instant
)
