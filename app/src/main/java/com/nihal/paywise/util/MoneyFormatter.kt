package com.nihal.paywise.util

import java.text.DecimalFormat
import java.util.*

object MoneyFormatter {
    private val indianFormat = DecimalFormat("##,##,##,##,##,##,##0.00")

    fun formatPaise(paise: Long, showSymbol: Boolean = true): String {
        val rupees = paise / 100.0
        val formatted = formatIndianStyle(rupees)
        return if (showSymbol) "₹$formatted" else formatted
    }

    /**
     * Formats number in Indian numbering system (Lakhs, Crores)
     * e.g., 1234567.89 -> 12,34,567.89
     */
    private fun formatIndianStyle(amount: Double): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)
        
        val parts = DecimalFormat("0.00").format(absAmount).split(".")
        var whole = parts[0]
        val decimal = parts[1]

        val result = StringBuilder()
        if (whole.length > 3) {
            val lastThree = whole.substring(whole.length - 3)
            val rest = whole.substring(0, whole.length - 3)
            result.append(formatRest(rest)).append(",").append(lastThree)
        } else {
            result.append(whole)
        }

        val finalStr = "${if (isNegative) "-" else ""}$result.$decimal"
        return finalStr
    }

    private fun formatRest(rest: String): String {
        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count == 2 && i > 0) {
                sb.append(",")
                count = 0
            }
        }
        return sb.reverse().toString()
    }
}
