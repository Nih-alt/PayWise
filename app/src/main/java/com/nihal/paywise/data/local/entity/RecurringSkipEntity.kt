package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_skips")
data class RecurringSkipEntity(
    @PrimaryKey
    val id: String, // Composite key: "recurringId|yearMonth"
    val recurringId: String,
    val yearMonth: String // yyyy-MM
)