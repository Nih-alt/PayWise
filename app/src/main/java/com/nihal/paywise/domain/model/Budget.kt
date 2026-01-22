package com.nihal.paywise.domain.model

import java.time.Instant

data class Budget(
    val id: String,
    val yearMonth: String,
    val categoryId: String?,
    val amountPaise: Long,
    val updatedAt: Instant
)
