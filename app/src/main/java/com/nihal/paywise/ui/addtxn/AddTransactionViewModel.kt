package com.nihal.paywise.ui.addtxn

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nihal.paywise.data.local.DatabaseSeeder
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.util.BudgetCheckWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModel(
    private val application: Application,
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

    var transactionType by mutableStateOf(TransactionType.EXPENSE)
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccountsStream()
        .onEach { Log.d(TAG, "Accounts count: ${it.size}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = snapshotFlow { transactionType }
        .flatMapLatest { type ->
            val kind = if (type == TransactionType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
            categoryRepository.getCategoriesByKindStream(kind)
        }
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

    fun seedDefaults() {
        viewModelScope.launch {
            DatabaseSeeder(accountRepository, categoryRepository).seed()
        }
    }

    fun updateType(type: TransactionType) {
        transactionType = type
    }

    fun setSalaryPrefill(cycleLabel: String) {
        transactionType = TransactionType.INCOME
        note = "$cycleLabel Salary"
        
        viewModelScope.launch {
            val incomeCats = categoryRepository.getCategoriesByKindStream(CategoryKind.INCOME).first()
            selectedCategoryId = incomeCats.find { it.name.contains("Salary", ignoreCase = true) }?.id 
                ?: incomeCats.firstOrNull()?.id
            
            val accs = accountRepository.getAllAccountsStream().first()
            selectedAccountId = accs.find { it.type == AccountType.BANK }?.id ?: accs.firstOrNull()?.id
        }
    }

    private fun validate(): Boolean {
        val paise = convertRupeesToPaise(amountInput)
        return paise > 0 && selectedAccountId != null && (transactionType == TransactionType.TRANSFER || selectedCategoryId != null)
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        if (!validate()) return

        viewModelScope.launch {
            val amountPaise = convertRupeesToPaise(amountInput)
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amountPaise = amountPaise,
                timestamp = date,
                type = transactionType,
                accountId = selectedAccountId!!,
                counterAccountId = null,
                categoryId = selectedCategoryId,
                note = note.ifBlank { null },
                recurringId = null,
                splitOfTransactionId = null
            )
            transactionRepository.insertTransaction(transaction)
            
            if (transactionType == TransactionType.EXPENSE) {
                WorkManager.getInstance(application).enqueue(
                    OneTimeWorkRequestBuilder<BudgetCheckWorker>().build()
                )
            }
            
            onSuccess()
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