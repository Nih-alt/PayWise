package com.nihal.paywise.util

/**
 * Utility to format monetary amounts in paise to human-readable Indian Rupees string.
 */
object MoneyFormatter {
    /**
     * Formats Long paise to a string like "₹10.00".
     * Uses integer math to avoid floating point precision issues.
     */
    fun formatPaise(amountPaise: Long): String {
        val absolutePaise = if (amountPaise < 0) -amountPaise else amountPaise
        val rupees = absolutePaise / 100
        val paise = absolutePaise % 100
        val sign = if (amountPaise < 0) "-" else ""
        
        return "${sign}₹$rupees.${paise.toString().padStart(2, '0')}"
    }
}