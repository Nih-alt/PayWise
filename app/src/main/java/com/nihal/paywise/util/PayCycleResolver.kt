package com.nihal.paywise.util

import com.nihal.paywise.domain.model.SalarySettings
import java.time.*

data class PayCycleRange(
    val start: Instant,
    val end: Instant,
    val label: String,
    val cycleMonth: YearMonth
)

object PayCycleResolver {
    fun resolve(
        now: Instant = Instant.now(),
        settings: SalarySettings,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PayCycleRange {
        val currentDateTime = now.atZone(zoneId)
        val today = currentDateTime.toLocalDate()
        
        if (!settings.isEnabled) {
            val ym = YearMonth.from(today)
            return PayCycleRange(
                start = ym.atDay(1).atStartOfDay(zoneId).toInstant(),
                end = ym.atEndOfMonth().plusDays(1).atStartOfDay(zoneId).toInstant(),
                label = DateTimeFormatterUtil.formatYearMonth(ym),
                cycleMonth = ym
            )
        }

        val salaryDay = settings.salaryDay
        val currentYM = YearMonth.from(today)
        
        // Determine the actual day in this month (handling 29/30/31)
        fun getActualSalaryDay(ym: YearMonth, day: Int): Int {
            return if (day == 0) ym.lengthOfMonth() else day.coerceAtMost(ym.lengthOfMonth())
        }

        val actualDayInCurrentMonth = getActualSalaryDay(currentYM, salaryDay)
        
        // If today is past salary day, current cycle started on this month's salary day + 1
        // and ends on next month's salary day.
        // Wait, requirements say: 
        // Example (salary date=30): Dec 31 -> Jan 30 is "Jan cycle"
        // This means the cycle ends ON the salary date.
        // So start is (SalaryDate of prev month) + 1 day, end is (SalaryDate of current month) + 1 day (exclusive end).
        
        val cycleMonth: YearMonth
        val start: LocalDate
        val end: LocalDate

        if (today.dayOfMonth > actualDayInCurrentMonth) {
            // We are in the next cycle already
            cycleMonth = currentYM.plusMonths(1)
            val startYM = currentYM
            val endYM = cycleMonth
            start = startYM.atDay(getActualSalaryDay(startYM, salaryDay)).plusDays(1)
            end = endYM.atDay(getActualSalaryDay(endYM, salaryDay)).plusDays(1)
        } else {
            // We are in the current month's cycle
            cycleMonth = currentYM
            val startYM = currentYM.minusMonths(1)
            val endYM = currentYM
            start = startYM.atDay(getActualSalaryDay(startYM, salaryDay)).plusDays(1)
            end = endYM.atDay(getActualSalaryDay(endYM, salaryDay)).plusDays(1)
        }

        return PayCycleRange(
            start = start.atStartOfDay(zoneId).toInstant(),
            end = end.atStartOfDay(zoneId).toInstant(),
            label = "${DateTimeFormatterUtil.formatYearMonth(cycleMonth)} Cycle",
            cycleMonth = cycleMonth
        )
    }
}
