package com.nihal.paywise.domain.model

import java.time.YearMonth

data class CategoryBreakdownRow(
    val categoryId: String,
    val categoryName: String,
    val categoryColor: Long,
    val totalAmount: Long,
    val percentage: Float = 0f
)

data class SpendingGroupRow(
    val spendingGroup: SpendingGroup,
    val totalAmount: Long,
    val percentage: Float = 0f
)

data class MonthlyTrendRow(
    val yearMonth: YearMonth,
    val totalAmount: Long
)

enum class ReportRangePreset {
    THIS_MONTH, LAST_30_DAYS, YEAR_TO_DATE, LAST_12_MONTHS, CUSTOM
}