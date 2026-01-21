package com.nihal.paywise.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun formatPaise_formatsPositiveAmountCorrectly() {
        assertEquals("₹10.00", MoneyFormatter.formatPaise(1000))
        assertEquals("₹0.05", MoneyFormatter.formatPaise(5))
        assertEquals("₹1.23", MoneyFormatter.formatPaise(123))
        assertEquals("₹12345.67", MoneyFormatter.formatPaise(1234567))
    }

    @Test
    fun formatPaise_formatsZeroAmountCorrectly() {
        assertEquals("₹0.00", MoneyFormatter.formatPaise(0))
    }

    @Test
    fun formatPaise_formatsNegativeAmountCorrectly() {
        assertEquals("-₹10.00", MoneyFormatter.formatPaise(-1000))
        assertEquals("-₹0.05", MoneyFormatter.formatPaise(-5))
        assertEquals("-₹1.23", MoneyFormatter.formatPaise(-123))
    }

    @Test
    fun formatPaise_doesNotReturnDoubleCurrencySymbol() {
        val amounts = listOf(1000L, 5L, 0L, -1000L, 1234567L)
        for (amount in amounts) {
            val formattedString = MoneyFormatter.formatPaise(amount)
            assertFalse("Formatted string '$formattedString' should not start with '₹₹'", formattedString.startsWith("₹₹"))
            // Also check for "-₹₹" in case of negative numbers
            assertFalse("Formatted string '$formattedString' should not start with '-₹₹'", formattedString.startsWith("-₹₹"))
        }
    }
}
