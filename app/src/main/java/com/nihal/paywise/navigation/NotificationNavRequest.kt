package com.nihal.paywise.navigation

data class NotificationNavRequest(
    val target: String,
    val recurringId: String? = null
)
