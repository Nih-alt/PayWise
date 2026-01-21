package com.nihal.paywise.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RecurringTransactionUiModel(
    val id: String,
    val amountText: String,
    val recurringTitle: String,
    val categoryName: String,
    val accountName: String,
    val dateText: String,
    val timeText: String
)

class RecurringTransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val transactions: StateFlow<List<RecurringTransactionUiModel>> = combine(
        transactionRepository.getRecurringTransactionsStream(),
        recurringRepository.getAllRecurringStream(),
        accountRepository.getAllAccountsStream(),
        categoryRepository.getAllCategoriesStream()
    ) { txs, recurringItems, accounts, categories ->
        val recurringMap = recurringItems.associate { it.id to it.title }
        val accountMap = accounts.associate { it.id to it.name }
        val categoryMap = categories.associate { it.id to it.name }

        txs.map { tx ->
            RecurringTransactionUiModel(
                id = tx.id,
                amountText = MoneyFormatter.formatPaise(tx.amountPaise),
                recurringTitle = recurringMap[tx.recurringId] ?: "Deleted Recurring",
                categoryName = categoryMap[tx.categoryId] ?: "Uncategorized",
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
}
