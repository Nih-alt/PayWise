package com.nihal.paywise.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.UserPreferencesRepository
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.SalarySettings
import com.nihal.paywise.domain.model.TransactionType
import com.nihal.paywise.domain.usecase.GetBudgetStatusUseCase
import com.nihal.paywise.domain.usecase.RunRecurringAutoPostUseCase
import com.nihal.paywise.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

enum class HomeBudgetStatus {
    NOT_SET, ON_TRACK, NEAR_LIMIT, OVER_BUDGET
}

data class HomeTransactionUiModel(
    val id: String,
    val amountText: String,
    val categoryName: String,
    val accountName: String,
    val dateText: String,
    val timeText: String,
    val type: TransactionType
)

data class HomeSummaryUiModel(
    val totalExpense: String,
    val totalIncome: String,
    val savings: String,
    val budgetPercent: Int? = null,
    val budgetStatus: HomeBudgetStatus = HomeBudgetStatus.NOT_SET,
    val count: Int,
    val cycleLabel: String,
    val isSalaryCycle: Boolean
)

data class CategorySummaryUiModel(
    val name: String,
    val totalAmount: String,
    val progress: Float
)

data class HomePlannedVsActualUiModel(
    val plannedAmountText: String,
    val actualAmountText: String,
    val deltaText: String,
    val isOverPlanned: Boolean,
    val progress: Float
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val runRecurringAutoPostUseCase: RunRecurringAutoPostUseCase,
    private val getBudgetStatusUseCase: GetBudgetStatusUseCase
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runRecurringAutoPostUseCase()
        }
    }

    private val salarySettings = userPreferencesRepository.salarySettingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, SalarySettings())

    private val currentCycle = salarySettings.map { settings ->
        PayCycleResolver.resolve(settings = settings)
    }.distinctUntilChanged()

    val summary: StateFlow<HomeSummaryUiModel> = currentCycle.flatMapLatest { cycle ->
        combine(
            getBudgetStatusUseCase(cycle.cycleMonth, cycle.start, cycle.end),
            transactionRepository.getTransactionsBetweenStream(cycle.start, cycle.end)
        ) { budgetStatus, txs ->
            val totalSpent = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
            val totalIncome = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
            val savings = totalIncome - totalSpent
            
            val overall = budgetStatus.overall
            val (status, percent) = when {
                overall == null -> HomeBudgetStatus.NOT_SET to null
                overall.percentUsed >= 1.0f -> HomeBudgetStatus.OVER_BUDGET to (overall.percentUsed * 100).toInt()
                overall.percentUsed >= 0.8f -> HomeBudgetStatus.NEAR_LIMIT to (overall.percentUsed * 100).toInt()
                else -> HomeBudgetStatus.ON_TRACK to (overall.percentUsed * 100).toInt()
            }

            HomeSummaryUiModel(
                totalExpense = MoneyFormatter.formatPaise(totalSpent),
                totalIncome = MoneyFormatter.formatPaise(totalIncome),
                savings = MoneyFormatter.formatPaise(savings),
                budgetPercent = percent,
                budgetStatus = status,
                count = txs.size,
                cycleLabel = cycle.label,
                isSalaryCycle = salarySettings.value.isEnabled
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeSummaryUiModel("0.00", "0.00", "0.00", count = 0, cycleLabel = "", isSalaryCycle = false)
    )

    val plannedVsActual: StateFlow<HomePlannedVsActualUiModel?> = currentCycle.flatMapLatest { cycle ->
        combine(
            recurringRepository.getAllRecurringStream(),
            transactionRepository.getTransactionsBetweenStream(cycle.start, cycle.end)
        ) { recurringList, transactions ->
            val activeRecurring = recurringList.filter { it.status == com.nihal.paywise.domain.model.RecurringStatus.ACTIVE }
            // Sum amount for recurring items that have their dueDay within the cycle range
            // For simplicity, we assume recurring are monthly. 
            // In a real scenario, we'd check if they occur in the current cycle's date range.
            val plannedTotal = activeRecurring.sumOf { it.amountPaise }
            val actualSpentOnRecurring = transactions
                .filter { it.type == TransactionType.EXPENSE && it.recurringId != null }
                .sumOf { it.amountPaise }
            
            val delta = plannedTotal - actualSpentOnRecurring
            HomePlannedVsActualUiModel(
                plannedAmountText = MoneyFormatter.formatPaise(plannedTotal),
                actualAmountText = MoneyFormatter.formatPaise(actualSpentOnRecurring),
                deltaText = MoneyFormatter.formatPaise(kotlin.math.abs(delta)),
                isOverPlanned = delta < 0,
                progress = if (plannedTotal > 0) (actualSpentOnRecurring.toFloat() / plannedTotal).coerceIn(0f, 1.2f) else 0f
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categorySummary: StateFlow<List<CategorySummaryUiModel>> = currentCycle.flatMapLatest { cycle ->
        combine(
            transactionRepository.getTransactionsBetweenStream(cycle.start, cycle.end),
            categoryRepository.getAllCategoriesStream()
        ) { txs, categories ->
            val categoryMap = categories.associate { it.id to it.name }
            val expenses = txs.filter { it.type == TransactionType.EXPENSE }
            val totalMonthExpense = expenses.sumOf { it.amountPaise }.toFloat()

            if (totalMonthExpense == 0f) return@combine emptyList<CategorySummaryUiModel>()

            expenses.groupBy { it.categoryId }
                .map { (catId, items) ->
                    val catTotal = items.sumOf { it.amountPaise }
                    CategorySummaryUiModel(
                        name = categoryMap[catId] ?: "Other",
                        totalAmount = MoneyFormatter.formatPaise(catTotal),
                        progress = catTotal / totalMonthExpense
                    )
                }
                .sortedByDescending { it.progress }
                .take(3)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val transactions: StateFlow<List<HomeTransactionUiModel>> = currentCycle.flatMapLatest { cycle ->
        combine(
            transactionRepository.getTransactionsBetweenStream(cycle.start, cycle.end),
            accountRepository.getAllAccountsStream(),
            categoryRepository.getAllCategoriesStream()
        ) { txs, accounts, categories ->
            val accountMap = accounts.associate { it.id to it.name }
            val categoryMap = categories.associate { it.id to it.name }

            txs.map { tx ->
                HomeTransactionUiModel(
                    id = tx.id,
                    amountText = MoneyFormatter.formatPaise(tx.amountPaise),
                    categoryName = if (tx.type == TransactionType.TRANSFER) "Transfer" 
                                  else categoryMap[tx.categoryId] ?: "Uncategorized",
                    accountName = accountMap[tx.accountId] ?: "Unknown Account",
                    dateText = DateTimeFormatterUtil.formatDate(tx.timestamp),
                    timeText = DateTimeFormatterUtil.formatTime(tx.timestamp),
                    type = tx.type
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    
    val showSalaryPrompt: StateFlow<Boolean> = currentCycle.flatMapLatest { cycle ->
        transactionRepository.getTransactionsBetweenStream(cycle.start, cycle.end).map { txs ->
            val hasSalary = txs.any { it.type == TransactionType.INCOME && it.note?.contains("Salary", ignoreCase = true) == true }
            salarySettings.value.isEnabled && !hasSalary
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}