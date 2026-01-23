package com.nihal.paywise.domain.model

data class SalarySettings(
    val isEnabled: Boolean = false,
    val salaryDay: Int = 1, // 1 to 31, 0 for Last Day
)
