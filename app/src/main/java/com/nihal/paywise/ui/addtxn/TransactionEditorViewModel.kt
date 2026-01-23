package com.nihal.paywise.ui.addtxn

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.util.BudgetCheckWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class TransactionEditorUiState(
    val amountInput: String = "",
    val selectedAccountId: String? = null,
    val selectedCounterAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val date: Instant = Instant.now(),
    val note: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false
)

class TransactionEditorViewModel(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val transactionId: String? = savedStateHandle["transactionId"]
    private val initialType: String? = savedStateHandle["type"]

    var uiState by mutableStateOf(TransactionEditorUiState())
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<Category>> = snapshotFlow { uiState.transactionType }
        .flatMapLatest { type: TransactionType ->
            val kind = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
            categoryRepository.getCategoriesByKindStream(kind)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (transactionId != null) {
            loadTransaction(transactionId)
        } else if (initialType != null) {
            uiState = uiState.copy(transactionType = TransactionType.valueOf(initialType))
        }
    }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val txn = transactionRepository.getTransactionById(id)
            if (txn != null) {
                uiState = uiState.copy(
                    amountInput = (txn.amountPaise / 100.0).toString(),
                    selectedAccountId = txn.accountId,
                    selectedCounterAccountId = txn.counterAccountId,
                    selectedCategoryId = txn.categoryId,
                    date = txn.timestamp,
                    note = txn.note ?: "",
                    transactionType = txn.type,
                    isEditMode = true,
                    isLoading = false
                )
            }
        }
    }

    fun updateAmount(input: String) { uiState = uiState.copy(amountInput = input) }
    fun updateAccount(id: String) { uiState = uiState.copy(selectedAccountId = id) }
    fun updateCounterAccount(id: String) { uiState = uiState.copy(selectedCounterAccountId = id) }
    fun updateCategory(id: String) { uiState = uiState.copy(selectedCategoryId = id) }
    fun updateDate(newDate: Instant) { uiState = uiState.copy(date = newDate) }
    fun updateNote(input: String) { uiState = uiState.copy(note = input) }
    fun updateType(type: TransactionType) { uiState = uiState.copy(transactionType = type) }

    fun validate(): Boolean {
        val paise = convertRupeesToPaise(uiState.amountInput)
        val basic = paise > 0 && uiState.selectedAccountId != null
        return when (uiState.transactionType) {
            TransactionType.TRANSFER -> basic && uiState.selectedCounterAccountId != null && uiState.selectedAccountId != uiState.selectedCounterAccountId
            else -> basic && uiState.selectedCategoryId != null
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        if (!validate()) return
        viewModelScope.launch {
            val amountPaise = convertRupeesToPaise(uiState.amountInput)
            val transaction = Transaction(
                id = transactionId ?: UUID.randomUUID().toString(),
                amountPaise = amountPaise,
                timestamp = uiState.date,
                type = uiState.transactionType,
                accountId = uiState.selectedAccountId!!,
                counterAccountId = uiState.selectedCounterAccountId,
                categoryId = if (uiState.transactionType == TransactionType.TRANSFER) null else uiState.selectedCategoryId,
                note = uiState.note.ifBlank { null },
                recurringId = null,
                splitOfTransactionId = null
            )
            transactionRepository.insertTransaction(transaction)
            if (uiState.transactionType == TransactionType.EXPENSE) {
                WorkManager.getInstance(application).enqueue(OneTimeWorkRequestBuilder<BudgetCheckWorker>().build())
            }
            onSuccess()
        }
    }

    fun deleteTransaction(onSuccess: () -> Unit) {
        transactionId?.let { id ->
            viewModelScope.launch {
                val txn = transactionRepository.getTransactionById(id)
                if (txn != null) {
                    transactionRepository.deleteTransaction(txn)
                    onSuccess()
                }
            }
        }
    }

    private fun convertRupeesToPaise(rupees: String): Long {
        if (rupees.isBlank()) return 0L
        return try {
            val parts = rupees.split(".")
            val main = parts[0].toLongOrNull() ?: 0L
            val fractionStr = parts.getOrNull(1)?.take(2)?.padEnd(2, '0') ?: "00"
            val fraction = fractionStr.toLongOrNull() ?: 0L
            (main * 100) + fraction
        } catch (e: Exception) { 0L }
    }
}