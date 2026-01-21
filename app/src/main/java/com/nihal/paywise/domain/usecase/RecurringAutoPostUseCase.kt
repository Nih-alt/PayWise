package com.nihal.paywise.domain.usecase

import android.util.Log
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Recurring
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

class RecurringAutoPostUseCase(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository
) {
    private val TAG = "RecurringAutoPost"

    /**
     * Executes the auto-posting logic for a list of recurring transactions for a given month.
     */
    suspend fun execute(currentMonth: YearMonth, recurringList: List<Recurring>) {
        val today = LocalDate.now()
        val currentMonthStr = currentMonth.toString() // yyyy-MM
        Log.d(TAG, "Starting auto-post check for $currentMonthStr. Items to check: ${recurringList.size}")

        for (recurring in recurringList) {
            Log.d(TAG, "Checking recurring item: ${recurring.title} (ID: ${recurring.id})")
            
            // 1. If status != ACTIVE → skip
            if (recurring.status != RecurringStatus.ACTIVE) {
                Log.d(TAG, "  Skip: Status is ${recurring.status}")
                continue
            }

            // 2. If endYearMonth exists and current YM > end → skip
            val endStr = recurring.endYearMonth
            if (endStr != null && currentMonth.isAfter(YearMonth.parse(endStr))) {
                Log.d(TAG, "  Skip: Current month $currentMonth is after endYearMonth $endStr")
                continue
            }

            // 3. If lastPostedYearMonth == current YM → skip
            if (recurring.lastPostedYearMonth == currentMonthStr) {
                Log.d(TAG, "  Skip: Already posted or skipped for $currentMonthStr")
                continue
            }

            // 4. Check if a Transaction already exists with recurringId AND same YM
            val monthStart = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val monthEnd = currentMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
            
            if (transactionRepository.hasTransactionForRecurringInRange(recurring.id, monthStart, monthEnd)) {
                Log.d(TAG, "  Skip: Transaction already exists in DB for this month.")
                // Update local field to prevent re-querying in this session
                recurringRepository.updateRecurring(recurring.copy(lastPostedYearMonth = currentMonthStr))
                continue
            }

            // 5. Resolve due date
            val dueDate = RecurringDateResolver.resolve(currentMonth, recurring.dueDay)

            // 6. If today >= due date:
            if (!today.isBefore(dueDate)) {
                Log.d(TAG, "  Due date reached/passed ($dueDate). Posting transaction...")
                postTransaction(recurring, dueDate, currentMonthStr)
            } else {
                Log.d(TAG, "  Due date ($dueDate) not reached yet. Today is $today.")
            }
        }
    }

    private suspend fun postTransaction(recurring: Recurring, dueDate: LocalDate, currentMonthStr: String) {
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            amountPaise = recurring.amountPaise,
            timestamp = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            type = TransactionType.EXPENSE,
            accountId = recurring.accountId,
            counterAccountId = null,
            categoryId = recurring.categoryId,
            note = "Auto-posted: ${recurring.title}",
            recurringId = recurring.id,
            splitOfTransactionId = null
        )

        transactionRepository.insertTransaction(transaction)
        recurringRepository.updateRecurring(
            recurring.copy(lastPostedYearMonth = currentMonthStr)
        )
        Log.d(TAG, "  Successfully posted transaction for ${recurring.title}")
    }
}