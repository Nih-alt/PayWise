package com.nihal.paywise.domain.model

data class BudgetStatus(
    val budgetPaise: Long,
    val spentPaise: Long,
    val categoryId: String? = null // null for overall
) {
    val percentUsed: Float
        get() = if (budgetPaise > 0) spentPaise.toFloat() / budgetPaise.toFloat() else 0f

    val remainingPaise: Long
        get() = budgetPaise - spentPaise
}
