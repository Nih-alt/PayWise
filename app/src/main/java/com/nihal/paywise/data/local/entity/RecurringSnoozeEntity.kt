package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_snoozes")
data class RecurringSnoozeEntity(
    @PrimaryKey
    val id: String, // Composite: recurringId + "|" + yearMonth
    val recurringId: String,
    val yearMonth: String,
    val snoozedUntilEpochMillis: Long
)
