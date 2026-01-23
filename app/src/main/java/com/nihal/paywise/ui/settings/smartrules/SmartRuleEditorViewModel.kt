package com.nihal.paywise.ui.settings.smartrules

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.entity.SmartMatchMode
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.data.repository.SmartRuleRepository
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.SmartRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class SmartRuleEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val smartRuleRepository: SmartRuleRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val ruleId: String? = savedStateHandle["ruleId"]

    var matchText by mutableStateOf("")
    var matchMode by mutableStateOf(SmartMatchMode.CONTAINS)
    var selectedCategoryId by mutableStateOf<String?>(null)
    var selectedAccountId by mutableStateOf<String?>(null)
    var priority by mutableStateOf(1)
    var isEditMode by mutableStateOf(false)

    val accounts: StateFlow<List<Account>> = accountRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            ruleId?.let { id ->
                smartRuleRepository.observeAllRules().stateIn(viewModelScope).value.find { it.id == id }?.let { rule ->
                    matchText = rule.matchText
                    matchMode = rule.matchMode
                    selectedCategoryId = rule.outputCategoryId
                    selectedAccountId = rule.outputAccountId
                    priority = rule.priority
                    isEditMode = true
                }
            } ?: run {
                priority = smartRuleRepository.getMaxPriority() + 1
            }
        }
    }

    fun saveRule(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val rule = SmartRule(
                id = ruleId ?: UUID.randomUUID().toString(),
                matchText = matchText,
                matchMode = matchMode,
                outputCategoryId = selectedCategoryId,
                outputTagIds = emptyList(),
                outputAccountId = selectedAccountId,
                priority = priority,
                enabled = true,
                createdAt = System.currentTimeMillis()
            )
            smartRuleRepository.upsertRule(rule)
            onSuccess()
        }
    }
}
