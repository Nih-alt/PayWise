package com.nihal.paywise.domain.usecase

import android.util.Log
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.RecurringSkipRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.RecurringStatus
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.TransactionType
import com.nihal.paywise.util.RecurringDateResolver
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class RunRecurringAutoPostUseCase(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val recurringSkipRepository: RecurringSkipRepository
) {
    private val TAG = "RunRecurringAutoPost"

    suspend operator fun invoke(
        nowInstant: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) {
        val currentYearMonth = YearMonth.from(nowInstant.atZone(zoneId))
        val currentYearMonthStr = currentYearMonth.toString()
        val todayLocalDate = nowInstant.atZone(zoneId).toLocalDate()
        
        Log.d(TAG, "Starting auto-post check for $currentYearMonth. Today: $todayLocalDate")

        // Helper to parse YearMonth safely
        fun parseYearMonth(ym: String): YearMonth = YearMonth.parse(ym)

        val activeRecurringList = recurringRepository.getActiveRecurring()
        
        for (recurring in activeRecurringList) {
            // status == ACTIVE is guaranteed by getActiveRecurring() usually, but safe to assume or check.
            if (recurring.status != RecurringStatus.ACTIVE) continue 
            if (!recurring.autoPost) continue

            val startYM = parseYearMonth(recurring.startYearMonth)
            if (currentYearMonth.isBefore(startYM)) continue // startYearMonth > current YM -> skip
            
            if (recurring.endYearMonth != null) {
                val endYM = parseYearMonth(recurring.endYearMonth)
                if (currentYearMonth.isAfter(endYM)) continue // current YM > endYearMonth -> skip
            }

            // Check if skipped for this month
            if (recurringSkipRepository.isSkipped(recurring.id, currentYearMonthStr)) {
                Log.d(TAG, "Skipping ${recurring.title}: User skipped for $currentYearMonth")
                continue
            }

            // Resolve due date
            val dueDateLocalDate = RecurringDateResolver.resolve(currentYearMonth, recurring.dueDay)
            
            // If todayLocalDate >= dueDateLocalDate
            if (!todayLocalDate.isBefore(dueDateLocalDate)) {
                // Check if transaction already exists
                val startOfMonth = currentYearMonth.atDay(1).atStartOfDay(zoneId).toInstant()
                val endOfMonth = currentYearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zoneId).toInstant()
                
                val exists = transactionRepository.existsRecurringTransactionInYearMonth(
                    recurring.id,
                    startOfMonth,
                    endOfMonth
                )
                
                if (exists) {
                    Log.d(TAG, "Skipping ${recurring.title}: Transaction already exists for $currentYearMonth")
                    // Update lastPostedYearMonth if needed for consistency
                    if (recurring.lastPostedYearMonth != currentYearMonth.toString()) {
                         recurringRepository.updateRecurring(recurring.copy(lastPostedYearMonth = currentYearMonth.toString()))
                    }
                    continue
                }

                // Create Transaction
                val newTransaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    amountPaise = recurring.amountPaise,
                    timestamp = nowInstant,
                    type = TransactionType.EXPENSE,
                    accountId = recurring.accountId,
                    counterAccountId = null,
                    categoryId = recurring.categoryId,
                    note = "Auto: ${recurring.title}",
                    recurringId = recurring.id,
                    splitOfTransactionId = null
                )
                
                Log.d(TAG, "Posting ${recurring.title} for $currentYearMonth (Due: $dueDateLocalDate)")
                transactionRepository.insertTransaction(newTransaction)
                
                // Update recurring lastPostedYearMonth
                recurringRepository.updateRecurring(recurring.copy(lastPostedYearMonth = currentYearMonth.toString()))
                
            } else {
                Log.d(TAG, "Skipping ${recurring.title}: Due date $dueDateLocalDate not reached yet")
            }
        }
    }
}
