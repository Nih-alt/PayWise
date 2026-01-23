package com.nihal.paywise.domain.model

import com.nihal.paywise.data.local.entity.SavingsGoalEntity

data class SavingsGoal(
    val id: String,
    val title: String,
    val targetAmountPaise: Long,
    val savedAmountPaise: Long = 0L,
    val targetDateEpochMillis: Long? = null,
    val color: Long,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remainingAmountPaise: Long = (targetAmountPaise - savedAmountPaise).coerceAtLeast(0L)
    val progressPercent: Float = if (targetAmountPaise > 0) savedAmountPaise.toFloat() / targetAmountPaise else 0f
}

fun SavingsGoalEntity.toDomain(saved: Long): SavingsGoal = SavingsGoal(
    id = id,
    title = title,
    targetAmountPaise = targetAmountPaise,
    savedAmountPaise = saved,
    targetDateEpochMillis = targetDateEpochMillis,
    color = color,
    isArchived = isArchived,
    createdAt = createdAt
)

fun SavingsGoal.toEntity(): SavingsGoalEntity = SavingsGoalEntity(
    id = id,
    title = title,
    targetAmountPaise = targetAmountPaise,
    targetDateEpochMillis = targetDateEpochMillis,
    color = color,
    isArchived = isArchived,
    createdAt = createdAt
)
