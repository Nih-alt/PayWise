package com.nihal.paywise.domain.usecase

import com.nihal.paywise.data.repository.SmartRuleRepository
import com.nihal.paywise.domain.model.SmartRule

class SmartRuleEngine(
    private val smartRuleRepository: SmartRuleRepository
) {
    /**
     * returns the best matching rule for the given input text (Merchant/Payee).
     */
    suspend fun getMatch(text: String): SmartRule? {
        return smartRuleRepository.findMatch(text)
    }
}
