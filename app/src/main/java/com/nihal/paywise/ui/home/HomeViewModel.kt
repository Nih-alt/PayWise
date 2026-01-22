package com.nihal.paywise.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.TransactionType
import com.nihal.paywise.domain.usecase.GetBudgetStatusUseCase
import com.nihal.paywise.domain.usecase.RunRecurringAutoPostUseCase
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
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
    val budgetPercent: Int? = null,
    val budgetStatus: HomeBudgetStatus = HomeBudgetStatus.NOT_SET,
    val count: Int
)

data class CategorySummaryUiModel(
    val name: String,
    val totalAmount: String,
    val progress: Float // 0.0 to 1.0
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val runRecurringAutoPostUseCase: RunRecurringAutoPostUseCase,
    private val getBudgetStatusUseCase: GetBudgetStatusUseCase
) : ViewModel() {

    private val currentMonthRange = calculateCurrentMonthRange()
    private val currentYearMonth = YearMonth.now()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runRecurringAutoPostUseCase()
        }
    }

    val summary: StateFlow<HomeSummaryUiModel> = getBudgetStatusUseCase(currentYearMonth)
        .combine(transactionRepository.getTransactionsBetweenStream(currentMonthRange.first, currentMonthRange.second)) { budgetStatus, txs ->
            val totalSpent = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
            
            val overall = budgetStatus.overall
            val (status, percent) = when {
                overall == null -> HomeBudgetStatus.NOT_SET to null
                overall.percentUsed >= 1.0f -> HomeBudgetStatus.OVER_BUDGET to (overall.percentUsed * 100).toInt()
                overall.percentUsed >= 0.8f -> HomeBudgetStatus.NEAR_LIMIT to (overall.percentUsed * 100).toInt()
                else -> HomeBudgetStatus.ON_TRACK to (overall.percentUsed * 100).toInt()
            }

            HomeSummaryUiModel(
                totalExpense = MoneyFormatter.formatPaise(totalSpent),
                budgetPercent = percent,
                budgetStatus = status,
                count = txs.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeSummaryUiModel("0.00", count = 0)
        )

    val categorySummary: StateFlow<List<CategorySummaryUiModel>> = combine(
        transactionRepository.getTransactionsBetweenStream(currentMonthRange.first, currentMonthRange.second),
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val transactions: StateFlow<List<HomeTransactionUiModel>> = combine(
        transactionRepository.getTransactionsBetweenStream(currentMonthRange.first, currentMonthRange.second),
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private fun calculateCurrentMonthRange(): Pair<java.time.Instant, java.time.Instant> {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        
        val endOfMonthExclusive = now.plusMonths(1).withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            
        return Pair(startOfMonth, endOfMonthExclusive)
    }
}
