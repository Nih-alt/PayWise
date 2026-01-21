package com.nihal.paywise.ui.recurring

enum class RecurringDisplayStatus {
    UPCOMING, DUE_TODAY, OVERDUE, PAID, SKIPPED
}

data class RecurringUiModel(
    val id: String,
    val title: String,
    val amountText: String,
    val dueDateText: String,
    val accountName: String,
    val categoryName: String,
    val status: RecurringDisplayStatus,
    val isPaused: Boolean,
    val isSkipped: Boolean
)