package com.nihal.paywise.ui.addtxn

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.DatabaseSeeder
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.CategoryKind
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val TAG = "AddTxnViewModel"

    var amountInput by mutableStateOf("")
        private set
    
    var selectedAccountId by mutableStateOf<String?>(null)
        private set
        
    var selectedCategoryId by mutableStateOf<String?>(null)
        private set

    var date by mutableStateOf(Instant.now())
        private set
        
    var note by mutableStateOf("")
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccountsStream()
        .onEach { Log.d(TAG, "Accounts count: ${it.size}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesByKindStream(CategoryKind.EXPENSE)
        .onEach { Log.d(TAG, "Categories (EXPENSE) count: ${it.size}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canSave: Boolean
        get() = validate()

    fun updateAmount(input: String) {
        amountInput = input
    }

    fun updateAccount(id: String) {
        selectedAccountId = id
    }

    fun updateCategory(id: String) {
        selectedCategoryId = id
    }

    fun updateDate(newDate: Instant) {
        date = newDate
    }

    fun updateNote(input: String) {
        note = input
    }

    private fun validate(): Boolean {
        val paise = convertRupeesToPaise(amountInput)
        return paise > 0 && selectedAccountId != null && selectedCategoryId != null
    }

    fun seedDefaults() {
        viewModelScope.launch {
            DatabaseSeeder(accountRepository, categoryRepository).seed()
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        if (!validate()) return

        viewModelScope.launch {
            val amountPaise = convertRupeesToPaise(amountInput)
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amountPaise = amountPaise,
                timestamp = date,
                type = TransactionType.EXPENSE,
                accountId = selectedAccountId!!,
                counterAccountId = null,
                categoryId = selectedCategoryId,
                note = note.ifBlank { null },
                recurringId = null,
                splitOfTransactionId = null
            )
            transactionRepository.insertTransaction(transaction)
            onSuccess()
        }
    }

    /**
     * Safely converts Rupees string to Long paise.
     * Handles "100", "100.5", "100.50", "100.05".
     */
    private fun convertRupeesToPaise(rupees: String): Long {
        if (rupees.isBlank()) return 0L
        
        return try {
            val parts = rupees.split(".")
            val main = parts[0].toLongOrNull() ?: 0L
            val fractionStr = parts.getOrNull(1)?.take(2)?.padEnd(2, '0') ?: "00"
            val fraction = fractionStr.toLongOrNull() ?: 0L
            
            (main * 100) + fraction
        } catch (e: Exception) {
            0L
        }
    }
}