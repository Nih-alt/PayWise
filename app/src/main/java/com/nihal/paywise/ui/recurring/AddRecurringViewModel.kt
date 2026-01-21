package com.nihal.paywise.ui.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.RecurringRepository
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.CategoryKind
import com.nihal.paywise.domain.model.Recurring
import com.nihal.paywise.domain.model.RecurringStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.UUID

class AddRecurringViewModel(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    var title by mutableStateOf("")
        private set
    var amountInput by mutableStateOf("")
        private set
    var dueDay by mutableIntStateOf(1)
        private set
    var selectedAccountId by mutableStateOf<String?>(null)
        private set
    var selectedCategoryId by mutableStateOf<String?>(null)
        private set
    var leadDays by mutableStateOf("3")
        private set
    var autoPost by mutableStateOf(true)
        private set

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getCategoriesByKindStream(CategoryKind.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canSave: Boolean
        get() = title.isNotBlank() && 
                convertRupeesToPaise(amountInput) > 0 && 
                selectedAccountId != null && 
                selectedCategoryId != null

    fun updateTitle(value: String) { title = value }
    fun updateAmount(value: String) { amountInput = value }
    fun updateDueDay(value: Int) { dueDay = value }
    fun updateAccount(id: String) { selectedAccountId = id }
    fun updateCategory(id: String) { selectedCategoryId = id }
    fun updateLeadDays(value: String) { leadDays = value }
    fun updateAutoPost(value: Boolean) { autoPost = value }

    fun saveRecurring(onSuccess: () -> Unit) {
        if (!canSave) return

        viewModelScope.launch {
            val recurring = Recurring(
                id = UUID.randomUUID().toString(),
                title = title,
                amountPaise = convertRupeesToPaise(amountInput),
                accountId = selectedAccountId!!,
                categoryId = selectedCategoryId!!,
                dueDay = dueDay,
                leadDays = leadDays.toIntOrNull() ?: 3,
                autoPost = autoPost,
                skipIfPaid = true,
                startYearMonth = YearMonth.now().toString(),
                endYearMonth = null,
                lastPostedYearMonth = null,
                status = RecurringStatus.ACTIVE
            )
            recurringRepository.insertRecurring(recurring)
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
        } catch (e: Exception) {
            0L
        }
    }
}

private fun mutableIntStateOf(i: Int) = androidx.compose.runtime.mutableIntStateOf(i)
