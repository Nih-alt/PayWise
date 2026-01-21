package com.nihal.paywise.ui.recurring

/**
 * UI model representing a single transaction row in the recurring history.
 */
data class RecurringHistoryRowUiModel(
    val transactionId: String,
    val amountText: String,
    val dateText: String,
    val timeText: String,
    val note: String?,
    val accountName: String,
    val categoryName: String
)

/**
 * UI model representing the header information for a specific recurring transaction.
 */
data class RecurringHistoryHeaderUiModel(
    val recurringTitle: String,
    val amountText: String,
    val accountName: String,
    val categoryName: String,
    val dueRuleText: String,
    val statusText: String,
    val totalPaidLast6: String
)
