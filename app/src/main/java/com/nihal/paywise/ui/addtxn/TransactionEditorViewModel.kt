package com.nihal.paywise.ui.addtxn

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nihal.paywise.data.local.entity.AttachmentEntity
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.AttachmentRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.domain.usecase.SmartRuleEngine
import com.nihal.paywise.util.BudgetCheckWorker
import com.nihal.paywise.util.FileHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.UUID

data class TransactionEditorUiState(
    val amountInput: String = "",
    val payee: String = "",
    val selectedAccountId: String? = null,
    val selectedCounterAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val date: Instant = Instant.now(),
    val note: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val attachments: List<AttachmentEntity> = emptyList(),
    val suggestedRule: SmartRule? = null
)

class TransactionEditorViewModel(
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val attachmentRepository: AttachmentRepository,
    private val smartRuleEngine: SmartRuleEngine
) : ViewModel() {

    private var currentTxnId: String = savedStateHandle["transactionId"] ?: UUID.randomUUID().toString()
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
        if (savedStateHandle.contains("transactionId")) {
            loadTransaction(currentTxnId)
        } else if (initialType != null) {
            uiState = uiState.copy(transactionType = TransactionType.valueOf(initialType))
        }
        
        observeAttachments(currentTxnId)
    }

    private fun loadTransaction(id: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val txn = transactionRepository.getTransactionById(id)
            if (txn != null) {
                uiState = uiState.copy(
                    amountInput = (txn.amountPaise / 100.0).toString(),
                    payee = txn.payee ?: "",
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

    private fun observeAttachments(txnId: String) {
        viewModelScope.launch {
            attachmentRepository.observeAttachmentsForTxn(txnId).collect {
                uiState = uiState.copy(attachments = it)
            }
        }
    }

    fun updatePayee(input: String) {
        uiState = uiState.copy(payee = input)
        viewModelScope.launch {
            val match = smartRuleEngine.getMatch(input)
            uiState = uiState.copy(suggestedRule = match)
        }
    }

    fun applySuggestion() {
        uiState.suggestedRule?.let { rule ->
            uiState = uiState.copy(
                selectedCategoryId = rule.outputCategoryId ?: uiState.selectedCategoryId,
                selectedAccountId = rule.outputAccountId ?: uiState.selectedAccountId,
                suggestedRule = null
            )
        }
    }

    fun addAttachment(uri: android.net.Uri) {
        val id = UUID.randomUUID().toString()
        val context = application.applicationContext
        val mimeType = context.contentResolver.getType(uri) ?: "image/*"
        val extension = if (mimeType == "application/pdf") "pdf" else "jpg"
        val relativePath = "attachments/$currentTxnId/$id.$extension"
        val targetFile = File(context.filesDir, relativePath)
        
        val bytes = FileHelper.copyUriToInternal(context, uri, targetFile)
        if (bytes > 0) {
            val attachment = AttachmentEntity(
                id = id,
                txnId = currentTxnId,
                storedRelativePath = relativePath,
                originalFileName = null,
                mimeType = mimeType,
                byteSize = bytes
            )
            viewModelScope.launch {
                attachmentRepository.insertAttachment(attachment)
            }
        }
    }

    fun removeAttachment(attachment: AttachmentEntity) {
        viewModelScope.launch {
            attachmentRepository.deleteAttachment(attachment)
            val file = File(application.filesDir, attachment.storedRelativePath)
            FileHelper.deleteFile(file)
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
                id = currentTxnId,
                amountPaise = amountPaise,
                timestamp = uiState.date,
                type = uiState.transactionType,
                accountId = uiState.selectedAccountId!!,
                counterAccountId = uiState.selectedCounterAccountId,
                categoryId = if (uiState.transactionType == TransactionType.TRANSFER) null else uiState.selectedCategoryId,
                note = uiState.note.ifBlank { null },
                payee = uiState.payee.ifBlank { null },
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
        viewModelScope.launch {
            val txn = transactionRepository.getTransactionById(currentTxnId)
            if (txn != null) {
                transactionRepository.deleteTransaction(txn)
                val folder = File(application.filesDir, "attachments/$currentTxnId")
                folder.deleteRecursively()
                onSuccess()
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