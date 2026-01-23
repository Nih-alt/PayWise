package com.nihal.paywise.ui.settings.smartrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.SmartRuleRepository
import com.nihal.paywise.domain.model.SmartRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartRulesViewModel(
    private val smartRuleRepository: SmartRuleRepository
) : ViewModel() {

    val allRules: StateFlow<List<SmartRule>> = smartRuleRepository.observeAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleRule(rule: SmartRule, enabled: Boolean) {
        viewModelScope.launch {
            smartRuleRepository.upsertRule(rule.copy(enabled = enabled))
        }
    }

    fun deleteRule(rule: SmartRule) {
        viewModelScope.launch {
            smartRuleRepository.deleteRule(rule)
        }
    }
}
