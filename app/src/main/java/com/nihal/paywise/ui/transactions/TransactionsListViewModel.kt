package com.nihal.paywise.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.util.*
import kotlinx.coroutines.flow.*
import java.time.*

data class TransactionsListUiState(
    val groupedTransactions: Map<LocalDate, List<TransactionListItem>> = emptyMap(),
    val typeFilter: TransactionType? = null,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val dateRange: Pair<Instant, Instant>? = null
)

data class TransactionListItem(
    val transaction: Transaction,
    val categoryName: String,
    val accountName: String,
    val amountText: String
)

class TransactionsListViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _typeFilter = MutableStateFlow<TransactionType?>(null)
    private val _accountIdFilter = MutableStateFlow<String?>(null)
    private val _categoryIdFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _dateRange = MutableStateFlow<Pair<Instant, Instant>?>(null)

    val accounts = accountRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryRepository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TransactionsListUiState> = combine(
        transactionRepository.getAllTransactionsStream(),
        accounts,
        categories,
        _typeFilter,
        _accountIdFilter,
        _categoryIdFilter,
        _searchQuery,
        _dateRange
    ) { flows ->
        val transactions = flows[0] as List<Transaction>
        val accountsList = flows[1] as List<Account>
        val categoriesList = flows[2] as List<Category>
        val type = flows[3] as TransactionType?
        val accId = flows[4] as String?
        val catId = flows[5] as String?
        val query = flows[6] as String
        val range = flows[7] as Pair<Instant, Instant>?

        val accountMap = accountsList.associateBy { it.id }
        val categoryMap = categoriesList.associateBy { it.id }

        val filtered = transactions.filter { txn ->
            val matchType = type == null || txn.type == type
            val matchAccount = accId == null || txn.accountId == accId || txn.counterAccountId == accId
            val matchCategory = catId == null || txn.categoryId == catId
            val matchSearch = query.isBlank() || (txn.note?.contains(query, ignoreCase = true) ?: false) || 
                             (categoryMap[txn.categoryId]?.name?.contains(query, ignoreCase = true) ?: false) ||
                             (accountMap[txn.accountId]?.name?.contains(query, ignoreCase = true) ?: false)
            val matchDate = range == null || (txn.timestamp >= range.first && txn.timestamp < range.second)
            
            matchType && matchAccount && matchCategory && matchSearch && matchDate
        }

        val listItems = filtered.map { txn ->
            TransactionListItem(
                transaction = txn,
                categoryName = if (txn.type == TransactionType.TRANSFER) "Transfer" else categoryMap[txn.categoryId]?.name ?: "Uncategorized",
                accountName = accountMap[txn.accountId]?.name ?: "Unknown",
                amountText = MoneyFormatter.formatPaise(txn.amountPaise)
            )
        }

        val grouped = listItems.groupBy { it.transaction.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }

        TransactionsListUiState(
            groupedTransactions = grouped,
            typeFilter = type,
            selectedAccountId = accId,
            selectedCategoryId = catId,
            searchQuery = query,
            dateRange = range
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TransactionsListUiState(isLoading = true))

    fun setTypeFilter(type: TransactionType?) { _typeFilter.value = type }
    fun setAccountFilter(id: String?) { _accountIdFilter.value = id }
    fun setCategoryFilter(id: String?) { _categoryIdFilter.value = id }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setDateRange(range: Pair<Instant, Instant>?) { _dateRange.value = range }
}
