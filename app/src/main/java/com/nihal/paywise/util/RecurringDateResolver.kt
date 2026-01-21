package com.nihal.paywise.util

import java.time.LocalDate
import java.time.YearMonth

/**
 * Utility to resolve a specific [LocalDate] from a [YearMonth] and a relative due day.
 */
object RecurringDateResolver {

    private const val LAST_DAY_OF_MONTH = -1

    fun resolve(yearMonth: YearMonth, dueDay: Int): LocalDate {
        val lastDay = yearMonth.lengthOfMonth()
        
        return if (dueDay == LAST_DAY_OF_MONTH || dueDay > lastDay) {
            yearMonth.atDay(lastDay)
        } else if (dueDay < 1) {
            yearMonth.atDay(1)
        } else {
            yearMonth.atDay(dueDay)
        }
    }
}
