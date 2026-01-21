package com.nihal.paywise.ui.recurring

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.entity.RecurringSnoozeEntity
import com.nihal.paywise.data.local.entity.RecurringSkipEntity
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.RecurringSkipRepository
import com.nihal.paywise.data.repository.RecurringSnoozeRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.Recurring
import com.nihal.paywise.domain.model.RecurringStatus
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.usecase.MarkRecurringAsPaidUseCase
import com.nihal.paywise.domain.usecase.SkipRecurringForMonthUseCase
import com.nihal.paywise.domain.usecase.UndoMarkRecurringAsPaidUseCase
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import com.nihal.paywise.util.RecurringDateResolver
import com.nihal.paywise.util.RecurringReminderScheduler
import com.nihal.paywise.util.RecurringStatusResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class RecurringCore(
    val id: String,
    val title: String,
    val dueDay: Int,
    val leadDays: Int,
    val status: RecurringStatus,
    val autoPost: Boolean,
    val startYearMonth: String,
    val endYearMonth: String?,
    val amountPaise: Long,
    val accountId: String,
    val categoryId: String
)

data class ScheduleSnapshot(
    val yearMonth: YearMonth,
    val recurrings: List<RecurringCore>,
    val skippedIds: Set<String>,
    val snoozeMap: Map<String, Long>,
    val paidIds: Set<String>
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
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

    private val TAG = "RecurringListVM"

    private val _currentMonth = MutableStateFlow(YearMonth.now(ZoneId.systemDefault()))
    val currentMonthFlow = _currentMonth.asStateFlow()

    fun refreshCurrentMonth() {
        val nowYm = YearMonth.now(ZoneId.systemDefault())
        if (_currentMonth.value != nowYm) {
            _currentMonth.value = nowYm
        }
    }

    private val monthWindowFlow = currentMonthFlow.map { ym ->
        val zone = ZoneId.systemDefault()
        val start = ym.atDay(1).atStartOfDay(zone).toInstant()
        val endExclusive = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        Triple(ym, start, endExclusive)
    }

    val displayMonth: StateFlow<String> = currentMonthFlow.map { 
        DateTimeFormatterUtil.formatYearMonth(it) 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    enum class ConfirmationType { NONE, PAID, SKIP }

    var itemToConfirm by mutableStateOf<RecurringUiModel?>(null)
        private set

    var confirmationType by mutableStateOf(ConfirmationType.NONE)
        private set

    var isSaving by mutableStateOf(false)
        private set

    private val _undoEvent = MutableSharedFlow<Pair<String, String>>() // txId, recurringId
    val undoEvent: SharedFlow<Pair<String, String>> = _undoEvent.asSharedFlow()

    init {
        // Dedicated scheduling flow to avoid spamming AlarmManager
        monthWindowFlow.flatMapLatest { (currentMonth, monthStart, monthEndExclusive) ->
            combine(
                recurringRepository.getAllRecurringStream(),
                transactionRepository.getTransactionsBetweenStream(monthStart, monthEndExclusive),
                recurringSkipRepository.getSkipsForYearMonthStream(currentMonth.toString()),
                recurringSnoozeRepository.observeForYearMonth(currentMonth.toString())
            ) { flows ->
                @Suppress("UNCHECKED_CAST")
                val recurringItems = flows[0] as List<Recurring>
                @Suppress("UNCHECKED_CAST")
                val transactions = flows[1] as List<Transaction>
                @Suppress("UNCHECKED_CAST")
                val skips = flows[2] as List<RecurringSkipEntity>
                @Suppress("UNCHECKED_CAST")
                val snoozes = flows[3] as List<RecurringSnoozeEntity>

                ScheduleSnapshot(
                    yearMonth = currentMonth,
                    recurrings = recurringItems.map { 
                        RecurringCore(
                            it.id, it.title, it.dueDay, it.leadDays, it.status, 
                            it.autoPost, it.startYearMonth, it.endYearMonth, 
                            it.amountPaise, it.accountId, it.categoryId
                        ) 
                    },
                    skippedIds = skips.map { it.recurringId }.toSet(),
                    snoozeMap = snoozes.associate { it.recurringId to it.snoozedUntilEpochMillis },
                    paidIds = transactions.mapNotNull { it.recurringId }.toSet()
                )
            }
        }
        .distinctUntilChanged()
        .debounce(500)
        .onEach { snapshot ->
            Log.d(TAG, "Snapshot changed -> Rescheduling reminders for ${snapshot.yearMonth} (${snapshot.recurrings.size} items)")
            withContext(Dispatchers.IO) {
                val domainRecurrings = snapshot.recurrings.map { 
                    Recurring(
                        it.id, it.title, it.amountPaise, it.accountId, it.categoryId,
                        it.dueDay, it.leadDays, it.autoPost, skipIfPaid = true,
                        it.startYearMonth, it.endYearMonth, lastPostedYearMonth = null, 
                        it.status
                    )
                }
                recurringReminderScheduler.scheduleForYearMonth(
                    yearMonth = snapshot.yearMonth,
                    recurrings = domainRecurrings,
                    skippedIds = snapshot.skippedIds,
                    snoozes = snapshot.snoozeMap,
                    paidIds = snapshot.paidIds
                )
            }
        }
        .launchIn(viewModelScope)
    }

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

    val recurringList: StateFlow<List<RecurringUiModel>> = monthWindowFlow.flatMapLatest { (currentMonth, monthStart, monthEndExclusive) ->
        combine(
            recurringRepository.getAllRecurringStream(),
            transactionRepository.getTransactionsBetweenStream(monthStart, monthEndExclusive),
            accountRepository.getAllAccountsStream(),
            categoryRepository.getAllCategoriesStream(),
            recurringSkipRepository.getSkipsForYearMonthStream(currentMonth.toString()),
            recurringSnoozeRepository.observeForYearMonth(currentMonth.toString())
        ) { flows ->
            @Suppress("UNCHECKED_CAST")
            val recurringItems = flows[0] as List<Recurring>
            @Suppress("UNCHECKED_CAST")
            val transactions = flows[1] as List<Transaction>
            @Suppress("UNCHECKED_CAST")
            val accounts = flows[2] as List<Account>
            @Suppress("UNCHECKED_CAST")
            val categories = flows[3] as List<Category>
            @Suppress("UNCHECKED_CAST")
            val skips = flows[4] as List<RecurringSkipEntity>
            @Suppress("UNCHECKED_CAST")
            val snoozes = flows[5] as List<RecurringSnoozeEntity>

            val paidRecurringIds = transactions.mapNotNull { it.recurringId }.toSet()
            val skippedRecurringIds = skips.map { it.recurringId }.toSet()
            val snoozeMap = snoozes.associate { it.recurringId to it.snoozedUntilEpochMillis }
            val accountMap = accounts.associate { it.id to it.name }
            val categoryMap = categories.associate { it.id to it.name }
            val today = LocalDate.now(ZoneId.systemDefault())

            recurringItems.map { item ->
                // Do not use lastPostedYearMonth for paid status; it can get stale if user deletes payments.
                val isPaid = paidRecurringIds.contains(item.id)
                val isSkipped = skippedRecurringIds.contains(item.id)
                
                // If paid this month, show next month's due date
                val dispMonth = if (isPaid) currentMonth.plusMonths(1) else currentMonth
                val dueDate = RecurringDateResolver.resolve(dispMonth, item.dueDay)
                val prefix = if (isPaid) "Next Due: " else "Due: "
                
                val (status, detail) = RecurringStatusResolver.resolve(
                    recurringId = item.id,
                    recurringStatus = item.status,
                    isSkipped = isSkipped,
                    isPaid = isPaid,
                    snoozedUntil = snoozeMap[item.id],
                    dueDate = RecurringDateResolver.resolve(currentMonth, item.dueDay),
                    today = today
                )

                RecurringUiModel(
                    id = item.id,
                    title = item.title,
                    amountText = MoneyFormatter.formatPaise(item.amountPaise),
                    dueDateText = prefix + DateTimeFormatterUtil.formatDate(dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                    accountName = accountMap[item.accountId] ?: "Unknown Account",
                    categoryName = categoryMap[item.categoryId] ?: "Uncategorized",
                    status = status,
                    isPaused = item.status == RecurringStatus.PAUSED,
                    isSkipped = isSkipped,
                    statusDetail = detail
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun markAsPaid(recurringId: String) {
        viewModelScope.launch {
            isSaving = true
            val ym = _currentMonth.value
            val transactionId = markRecurringAsPaidUseCase.execute(recurringId, ym)
            recurringSnoozeRepository.delete(recurringId, ym.toString())
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
            val ymStr = _currentMonth.value.toString()
            recurringRepository.updateRecurring(recurring.copy(status = newStatus))
            if (newStatus == RecurringStatus.PAUSED) {
                recurringSnoozeRepository.delete(recurringId, ymStr)
            }
        }
    }

    fun skipThisMonth(recurringId: String) {
        viewModelScope.launch {
            val ym = _currentMonth.value
            skipRecurringForMonthUseCase.execute(recurringId, ym)
            recurringSnoozeRepository.delete(recurringId, ym.toString())
            itemToConfirm = null
            confirmationType = ConfirmationType.NONE
        }
    }

    fun unskipThisMonth(recurringId: String) {
        viewModelScope.launch {
            val ym = _currentMonth.value
            skipRecurringForMonthUseCase.unskip(recurringId, ym)
            recurringSnoozeRepository.delete(recurringId, ym.toString())
        }
    }
}
