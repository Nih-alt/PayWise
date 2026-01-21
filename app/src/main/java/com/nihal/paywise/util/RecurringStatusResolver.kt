package com.nihal.paywise.util

import com.nihal.paywise.ui.recurring.RecurringDisplayStatus
import java.time.LocalDate

object RecurringStatusResolver {
    /**
     * Resolves the display status for a recurring transaction.
     */
    fun resolve(
        dueDate: LocalDate,
        today: LocalDate,
        isPaid: Boolean
    ): RecurringDisplayStatus {
        return when {
            isPaid -> RecurringDisplayStatus.PAID
            today.isBefore(dueDate) -> RecurringDisplayStatus.UPCOMING
            today.isEqual(dueDate) -> RecurringDisplayStatus.DUE_TODAY
            else -> RecurringDisplayStatus.OVERDUE
        }
    }
}
