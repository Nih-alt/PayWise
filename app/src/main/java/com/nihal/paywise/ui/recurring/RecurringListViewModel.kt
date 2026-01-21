package com.nihal.paywise.ui.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.RecurringSkipRepository
import com.nihal.paywise.data.repository.RecurringSnoozeRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.data.local.entity.RecurringSnoozeEntity
import com.nihal.paywise.data.local.entity.TransactionEntity
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.Recurring
import com.nihal.paywise.domain.model.RecurringStatus
import com.nihal.paywise.domain.usecase.MarkRecurringAsPaidUseCase
import com.nihal.paywise.domain.usecase.SkipRecurringForMonthUseCase
import com.nihal.paywise.domain.usecase.UndoMarkRecurringAsPaidUseCase
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import com.nihal.paywise.util.RecurringDateResolver
import com.nihal.paywise.util.RecurringStatusResolver
import com.nihal.paywise.util.RecurringReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class RecurringListViewModel(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringSkipRepository: RecurringSkipRepository,
    private val recurringSnoozeRepository: RecurringSnoozeRepository,
    private val recurringReminderScheduler: RecurringReminderScheduler,
    private val markRecurringAsPaidUseCase: MarkRecurringAsPaidUseCase,
    private val undoMarkRecurringAsPaidUseCase: UndoMarkRecurringAsPaidUseCase,
    private val skipRecurringForMonthUseCase: SkipRecurringForMonthUseCase
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val monthStart = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
    private val monthEnd = currentMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()

    val displayMonth: String = DateTimeFormatterUtil.formatYearMonth(currentMonth)

    enum class ConfirmationType { NONE, PAID, SKIP }

    var itemToConfirm by mutableStateOf<RecurringUiModel?>(null)
        private set

    var confirmationType by mutableStateOf(ConfirmationType.NONE)
        private set

    var isSaving by mutableStateOf(false)
        private set

    private val _undoEvent = MutableSharedFlow<Pair<String, String>>() // txId, recurringId
    val undoEvent: SharedFlow<Pair<String, String>> = _undoEvent.asSharedFlow()

    fun showConfirmDialog(item: RecurringUiModel) {
        itemToConfirm = item
        confirmationType = ConfirmationType.PAID
    }
    
    fun showSkipConfirmDialog(item: RecurringUiModel) {
        itemToConfirm = item
        confirmationType = ConfirmationType.SKIP
    }

    fun dismissConfirmDialog() {
        itemToConfirm = null
        confirmationType = ConfirmationType.NONE
    }

    val recurringList: StateFlow<List<RecurringUiModel>> = combine(
        recurringRepository.getAllRecurringStream(),
        transactionRepository.getTransactionsBetweenStream(monthStart, monthEnd),
        accountRepository.getAllAccountsStream(),
        categoryRepository.getAllCategoriesStream(),
        recurringSkipRepository.getSkipsForYearMonthStream(currentMonth.toString()),
        recurringSnoozeRepository.observeForYearMonth(currentMonth.toString())
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val recurringItems = flows[0] as List<Recurring>
        @Suppress("UNCHECKED_CAST")
        val transactions = flows[1] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = flows[2] as List<Account>
        @Suppress("UNCHECKED_CAST")
        val categories = flows[3] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val skips = flows[4] as List<com.nihal.paywise.data.local.entity.RecurringSkipEntity>
        @Suppress("UNCHECKED_CAST")
        val snoozes = flows[5] as List<RecurringSnoozeEntity>

        val paidRecurringIds = transactions.mapNotNull { it.recurringId }.toSet()
        val skippedRecurringIds = skips.map { it.recurringId }.toSet()
        val snoozeMap = snoozes.associate { it.recurringId to it.snoozedUntilEpochMillis }
        val accountMap = accounts.associate { it.id to it.name }
        val categoryMap = categories.associate { it.id to it.name }
        val today = LocalDate.now()

        // Schedule reminders for the current view state
        recurringReminderScheduler.scheduleForYearMonth(currentMonth, recurringItems, skippedRecurringIds, snoozeMap)

        recurringItems.map { item ->
            val isPaid = paidRecurringIds.contains(item.id) || item.lastPostedYearMonth == currentMonth.toString()
            val isSkipped = skippedRecurringIds.contains(item.id)
            
            // If paid this month, show next month's due date
            val displayMonth = if (isPaid) currentMonth.plusMonths(1) else currentMonth
            val dueDate = RecurringDateResolver.resolve(displayMonth, item.dueDay)
            val prefix = if (isPaid) "Next Due: " else "Due: "
            
            val baseStatus = RecurringStatusResolver.resolve(
                RecurringDateResolver.resolve(currentMonth, item.dueDay), 
                today, 
                isPaid
            )

            val status = when {
                item.status == RecurringStatus.PAUSED -> RecurringDisplayStatus.UPCOMING 
                isSkipped -> RecurringDisplayStatus.SKIPPED
                else -> baseStatus
            }

            RecurringUiModel(
                id = item.id,
                title = item.title,
                amountText = MoneyFormatter.formatPaise(item.amountPaise),
                dueDateText = prefix + DateTimeFormatterUtil.formatDate(dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                accountName = accountMap[item.accountId] ?: "Unknown Account",
                categoryName = categoryMap[item.categoryId] ?: "Uncategorized",
                status = status,
                isPaused = item.status == RecurringStatus.PAUSED,
                isSkipped = isSkipped
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun markAsPaid(recurringId: String) {
        viewModelScope.launch {
            isSaving = true
            val transactionId = markRecurringAsPaidUseCase.execute(recurringId, currentMonth)
            recurringSnoozeRepository.delete(recurringId, currentMonth.toString())
            isSaving = false
            itemToConfirm = null
            confirmationType = ConfirmationType.NONE
            
            if (transactionId != null) {
                _undoEvent.emit(transactionId to recurringId)
            }
        }
    }

    fun undoMarkAsPaid(transactionId: String, recurringId: String) {
        viewModelScope.launch {
            undoMarkRecurringAsPaidUseCase.execute(transactionId, recurringId)
        }
    }

    fun toggleStatus(recurringId: String) {
        viewModelScope.launch {
            val recurring = recurringRepository.getRecurringById(recurringId) ?: return@launch
            val newStatus = if (recurring.status == RecurringStatus.ACTIVE) RecurringStatus.PAUSED else RecurringStatus.ACTIVE
            recurringRepository.updateRecurring(recurring.copy(status = newStatus))
            if (newStatus == RecurringStatus.PAUSED) {
                recurringSnoozeRepository.delete(recurringId, currentMonth.toString())
            }
        }
    }

    fun skipThisMonth(recurringId: String) {
        viewModelScope.launch {
            skipRecurringForMonthUseCase.execute(recurringId, currentMonth)
            recurringSnoozeRepository.delete(recurringId, currentMonth.toString())
            itemToConfirm = null
            confirmationType = ConfirmationType.NONE
        }
    }

    fun unskipThisMonth(recurringId: String) {
        viewModelScope.launch {
            skipRecurringForMonthUseCase.unskip(recurringId, currentMonth)
            recurringSnoozeRepository.delete(recurringId, currentMonth.toString())
        }
    }
}