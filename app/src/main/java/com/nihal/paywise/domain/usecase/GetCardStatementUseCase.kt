package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.*

class GetCardStatementUseCase(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(account: Account, yearMonth: YearMonth): Flow<CardStatement?> {
        if (account.type != AccountType.CARD || account.statementDay == null || account.dueDay == null) {
            return kotlinx.coroutines.flow.flowOf(null)
        }

        val zone = ZoneId.systemDefault()
        
        // Resolve Period
        val statementDay = account.statementDay
        val currentMonthEnd = resolveDate(yearMonth, statementDay)
        val prevMonthEnd = resolveDate(yearMonth.minusMonths(1), statementDay)
        
        val startInstant = prevMonthEnd.plusDays(1).atStartOfDay(zone).toInstant()
        val endInstant = currentMonthEnd.plusDays(1).atStartOfDay(zone).toInstant() // Exclusive

        // Resolve Due Date
        val dueDay = account.dueDay
        var dueDate = resolveDate(yearMonth, dueDay)
        if (dueDay < statementDay) {
            dueDate = resolveDate(yearMonth.plusMonths(1), dueDay)
        }

        return transactionRepository.getTransactionsBetweenStream(startInstant, endInstant).map { transactions ->
            val charges = transactions
                .filter { it.accountId == account.id && it.type == TransactionType.EXPENSE }
                .sumOf { it.amountPaise }
            
            val payments = transactions
                .filter { it.counterAccountId == account.id && it.type == TransactionType.TRANSFER }
                .sumOf { it.amountPaise }
            
            val netDue = charges - payments
            
            val status = when {
                netDue <= 0 -> CardPaymentStatus.PAID
                LocalDate.now().isAfter(dueDate) -> CardPaymentStatus.OVERDUE
                LocalDate.now().plusDays(3).isAfter(dueDate) -> CardPaymentStatus.DUE_SOON
                else -> CardPaymentStatus.NO_DUE
            }

            CardStatement(
                accountId = account.id,
                accountName = account.name,
                statementPeriodStart = startInstant,
                statementPeriodEnd = endInstant,
                dueDate = dueDate,
                totalChargesPaise = charges,
                totalPaymentsPaise = payments,
                netDuePaise = netDue,
                status = status
            )
        }
    }

    private fun resolveDate(ym: YearMonth, day: Int): LocalDate {
        return if (day == 0) ym.atEndOfMonth() else ym.atDay(day.coerceAtMost(ym.lengthOfMonth()))
    }
}
