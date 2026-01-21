package com.nihal.paywise.ui.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.usecase.DeleteRecurringTransactionUseCase
import com.nihal.paywise.domain.usecase.SyncRecurringLastPostedMonthUseCase
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class RecurringHistoryViewModel(
    savedStateHandle: SavedStateHandle,
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase,
    private val syncRecurringLastPostedMonthUseCase: SyncRecurringLastPostedMonthUseCase
) : ViewModel() {

    private val recurringId: String = checkNotNull(savedStateHandle["recurringId"])

    var itemToDelete by mutableStateOf<RecurringHistoryRowUiModel?>(null)
        private set

    private var lastDeletedTransaction: Transaction? = null

    private val _undoDeleteEvent = MutableSharedFlow<Unit>()
    val undoDeleteEvent = _undoDeleteEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    fun confirmDelete(item: RecurringHistoryRowUiModel) {
        itemToDelete = item
    }

    fun dismissDelete() {
        itemToDelete = null
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionById(transactionId)
            if (tx != null) {
                lastDeletedTransaction = tx
                deleteRecurringTransactionUseCase.execute(transactionId, recurringId)
                _undoDeleteEvent.emit(Unit)
            }
            itemToDelete = null
        }
    }

    fun undoDelete() {
        val tx = lastDeletedTransaction ?: return
        viewModelScope.launch {
            try {
                transactionRepository.insertTransaction(tx)
                syncRecurringLastPostedMonthUseCase.execute(recurringId)
                lastDeletedTransaction = null
            } catch (e: Exception) {
                _errorEvent.emit("Failed to restore payment")
            }
        }
    }

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val currentMonth = YearMonth.now()
    private val startMonth = currentMonth.minusMonths(5)
    
    private val windowStart = startMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
    private val windowEndExclusive = currentMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

    val rows: StateFlow<List<RecurringHistoryRowUiModel>> = combine(
        transactionRepository.getTransactionsForRecurringInRangeStream(recurringId, windowStart, windowEndExclusive),
        accountRepository.getAllAccountsStream(),
        categoryRepository.getAllCategoriesStream()
    ) { txs, accounts, categories ->
        val accountMap = accounts.associate { it.id to it.name }
        val categoryMap = categories.associate { it.id to it.name }

        txs.map { tx ->
            RecurringHistoryRowUiModel(
                transactionId = tx.id,
                amountText = MoneyFormatter.formatPaise(tx.amountPaise),
                dateText = DateTimeFormatterUtil.formatDate(tx.timestamp),
                timeText = DateTimeFormatterUtil.formatTime(tx.timestamp),
                note = tx.note,
                accountName = accountMap[tx.accountId] ?: "Unknown Account",
                categoryName = categoryMap[tx.categoryId] ?: "Uncategorized"
            )
        }
    }.onEach { _loading.value = false }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val header: StateFlow<RecurringHistoryHeaderUiModel?> = combine(
        recurringRepository.getAllRecurringStream().map { list -> list.find { it.id == recurringId } },
        transactionRepository.getTransactionsForRecurringInRangeStream(recurringId, windowStart, windowEndExclusive),
        accountRepository.getAllAccountsStream(),
        categoryRepository.getAllCategoriesStream()
    ) { recurring, txs, accounts, categories ->
        if (recurring == null) return@combine null

        val accountMap = accounts.associate { it.id to it.name }
        val categoryMap = categories.associate { it.id to it.name }
        
        val totalPaise = txs.sumOf { it.amountPaise }
        
        // Do not use lastPostedYearMonth for paid status; it can get stale if user deletes payments.
        val hasPaidThisMonth = txs.any { 
            val ym = YearMonth.from(it.timestamp.atZone(ZoneId.systemDefault()))
            ym == currentMonth 
        }

        RecurringHistoryHeaderUiModel(
            recurringTitle = recurring.title,
            amountText = MoneyFormatter.formatPaise(recurring.amountPaise),
            accountName = accountMap[recurring.accountId] ?: "Unknown Account",
            categoryName = categoryMap[recurring.categoryId] ?: "Uncategorized",
            dueRuleText = formatDueRule(recurring.dueDay),
            statusText = if (hasPaidThisMonth) "Paid this month" else "Not paid this month",
            totalPaidLast6 = MoneyFormatter.formatPaise(totalPaise)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private fun formatDueRule(dueDay: Int): String {
        return if (dueDay == -1) "Due on last day"
        else "Due on day $dueDay"
    }
}
