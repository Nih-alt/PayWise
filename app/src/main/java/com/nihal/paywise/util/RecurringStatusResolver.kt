package com.nihal.paywise.util

import android.util.Log
import com.nihal.paywise.domain.model.RecurringStatus
import com.nihal.paywise.ui.recurring.RecurringDisplayStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object RecurringStatusResolver {
    /**
     * Resolves the display status for a recurring transaction with priority.
     */
    fun resolve(
        recurringId: String,
        recurringStatus: RecurringStatus,
        isSkipped: Boolean,
        isPaid: Boolean,
        snoozedUntil: Long?,
        dueDate: LocalDate,
        today: LocalDate
    ): Pair<RecurringDisplayStatus, String?> {
        val now = System.currentTimeMillis()
        
        val result = when {
            recurringStatus == RecurringStatus.PAUSED -> RecurringDisplayStatus.UPCOMING to "Paused"
            isSkipped -> RecurringDisplayStatus.SKIPPED to null
            isPaid -> RecurringDisplayStatus.PAID to null
            snoozedUntil != null && snoozedUntil > now -> {
                val snoozeInstant = java.time.Instant.ofEpochMilli(snoozedUntil)
                RecurringDisplayStatus.SNOOZED to "Snoozed until ${DateTimeFormatterUtil.formatTime(snoozeInstant)}"
            }
            today.isAfter(dueDate) -> {
                val days = ChronoUnit.DAYS.between(dueDate, today)
                RecurringDisplayStatus.OVERDUE to "$days days overdue"
            }
            today.isEqual(dueDate) -> RecurringDisplayStatus.DUE_TODAY to null
            else -> RecurringDisplayStatus.UPCOMING to null
        }
        
        Log.d("Status", "$recurringId -> ${result.first}")
        return result
    }
}
