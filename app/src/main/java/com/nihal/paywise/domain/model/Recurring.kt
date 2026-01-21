package com.nihal.paywise.domain.model

import com.nihal.paywise.data.local.entity.RecurringEntity

data class Recurring(
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
    val status: RecurringStatus
)

fun Recurring.toEntity(): RecurringEntity = RecurringEntity(
    id = id,
    title = title,
    amountPaise = amountPaise,
    accountId = accountId,
    categoryId = categoryId,
    dueDay = dueDay,
    leadDays = leadDays,
    autoPost = autoPost,
    skipIfPaid = skipIfPaid,
    startYearMonth = startYearMonth,
    endYearMonth = endYearMonth,
    lastPostedYearMonth = lastPostedYearMonth,
    status = status
)

fun RecurringEntity.toDomain(): Recurring = Recurring(
    id = id,
    title = title,
    amountPaise = amountPaise,
    accountId = accountId,
    categoryId = categoryId,
    dueDay = dueDay,
    leadDays = leadDays,
    autoPost = autoPost,
    skipIfPaid = skipIfPaid,
    startYearMonth = startYearMonth,
    endYearMonth = endYearMonth,
    lastPostedYearMonth = lastPostedYearMonth,
    status = status
)
