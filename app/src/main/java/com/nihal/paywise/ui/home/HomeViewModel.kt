package com.nihal.paywise.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.TransactionType
import com.nihal.paywise.domain.usecase.RunRecurringAutoPostUseCase
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class HomeTransactionUiModel(
    val id: String,
    val amountText: String,
    val categoryName: String,
    val accountName: String,
    val dateText: String,
    val timeText: String
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
                timeText = DateTimeFormatterUtil.formatTime(tx.timestamp)
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
        
        val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
            .atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            
        return Pair(startOfMonth, endOfMonth)
    }
}