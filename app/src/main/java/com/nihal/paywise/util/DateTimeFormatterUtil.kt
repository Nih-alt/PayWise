package com.nihal.paywise.util

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility to format java.time.Instant to local date and time strings.
 */
object DateTimeFormatterUtil {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val yearMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    /**
     * Returns a date string like "20 Jan 2026".
     */
    fun formatDate(timestamp: Instant): String {
        return timestamp.atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    /**
     * Returns a string like "Jan 2026" for a YearMonth.
     */
    fun formatYearMonth(yearMonth: YearMonth): String {
        return yearMonth.format(yearMonthFormatter)
    }

    /**
     * Returns a time string like "3:19 PM".
     */
    fun formatTime(timestamp: Instant): String {
        return timestamp.atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }
}