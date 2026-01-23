package com.nihal.paywise.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object CardCycleResolver {

    /**
     * Returns the start and end Instant for the current cycle based on today's date.
     */
    fun getCycleRange(anchorDate: LocalDate, statementDay: Int): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        
        // If today is after the statement day, we are in the cycle ending next month
        // If today is on or before, we are in the cycle ending this month
        val currentMonthStatementDate = normalizeDay(YearMonth.from(anchorDate), statementDay)
        
        val (start, end) = if (anchorDate.isAfter(currentMonthStatementDate)) {
            val cycleEnd = normalizeDay(YearMonth.from(anchorDate).plusMonths(1), statementDay)
            val cycleStart = currentMonthStatementDate.plusDays(1)
            cycleStart to cycleEnd
        } else {
            val cycleEnd = currentMonthStatementDate
            val cycleStart = normalizeDay(YearMonth.from(anchorDate).minusMonths(1), statementDay).plusDays(1)
            cycleStart to cycleEnd
        }

        return start.atStartOfDay(zone).toInstant() to end.plusDays(1).atStartOfDay(zone).toInstant()
    }

    fun getDueDate(statementDate: LocalDate, dueDay: Int): LocalDate {
        val nextMonth = YearMonth.from(statementDate).plusMonths(1)
        val candidate = normalizeDay(YearMonth.from(statementDate), dueDay)
        
        return if (dueDay < statementDate.dayOfMonth) {
            normalizeDay(nextMonth, dueDay)
        } else {
            candidate
        }
    }

    private fun normalizeDay(ym: YearMonth, day: Int): LocalDate {
        return ym.atDay(day.coerceAtMost(ym.lengthOfMonth()))
    }
}
