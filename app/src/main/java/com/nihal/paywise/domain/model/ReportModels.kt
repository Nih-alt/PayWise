package com.nihal.paywise.domain.model

data class CategoryReportItem(
    val categoryId: String,
    val categoryName: String,
    val amountPaise: Long,
    val percentage: Float
)

data class TrendItem(
    val label: String, // e.g., "Jan"
    val amountPaise: Long
)

data class FixedVsDiscretionaryReport(
    val fixedTotal: Long,
    val discretionaryTotal: Long
) {
    val total: Long get() = fixedTotal + discretionaryTotal
    val fixedPercent: Float get() = if (total > 0) fixedTotal.toFloat() / total.toFloat() else 0f
    val discretionaryPercent: Float get() = if (total > 0) discretionaryTotal.toFloat() / total.toFloat() else 0f
}
