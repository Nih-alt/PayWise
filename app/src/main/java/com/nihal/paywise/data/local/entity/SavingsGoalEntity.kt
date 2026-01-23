package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val targetAmountPaise: Long,
    val targetDateEpochMillis: Long?,
    val color: Long,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
