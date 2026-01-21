package com.nihal.paywise.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.TransactionType
import com.nihal.paywise.domain.usecase.RunRecurringAutoPostUseCase
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

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
    private val runRecurringAutoPostUseCase: RunRecurringAutoPostUseCase
) : ViewModel() {

    private val currentMonthRange = calculateCurrentMonthRange()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runRecurringAutoPostUseCase()
        }
    }

    val summary: StateFlow<HomeSummaryUiModel> = transactionRepository.getTransactionsBetweenStream(currentMonthRange.first, currentMonthRange.second)
        .map { txs: List<Transaction> ->
            val total = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }
            HomeSummaryUiModel(
                totalExpense = MoneyFormatter.formatPaise(total),
                count = txs.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeSummaryUiModel("0.00", 0)
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
