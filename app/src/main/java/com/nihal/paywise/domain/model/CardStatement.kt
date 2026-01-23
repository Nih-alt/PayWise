package com.nihal.paywise.domain.model

import java.time.Instant
import java.time.LocalDate

enum class CardPaymentStatus {
    PAID, DUE_SOON, OVERDUE, NO_DUE
}

data class CardStatement(
    val accountId: String,
    val accountName: String,
    val statementPeriodStart: Instant,
    val statementPeriodEnd: Instant,
    val dueDate: LocalDate,
    val totalChargesPaise: Long,
    val totalPaymentsPaise: Long,
    val netDuePaise: Long,
    val status: CardPaymentStatus
)
