package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.SmartRuleDao
import com.nihal.paywise.domain.model.SmartRule
import com.nihal.paywise.domain.model.toDomain
import com.nihal.paywise.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SmartRuleRepository {
    fun observeAllRules(): Flow<List<SmartRule>>
    suspend fun findMatch(text: String): SmartRule?
    suspend fun upsertRule(rule: SmartRule)
    suspend fun deleteRule(rule: SmartRule)
    suspend fun getMaxPriority(): Int
}

class OfflineSmartRuleRepository(
    private val smartRuleDao: SmartRuleDao
) : SmartRuleRepository {

    override fun observeAllRules(): Flow<List<SmartRule>> =
        smartRuleDao.observeAllRules().map { entities -> entities.map { it.toDomain() } }

    /**
     * iterates through enabled rules by priority and returns the first match.
     */
    override suspend fun findMatch(text: String): SmartRule? {
        if (text.isBlank()) return null
        val enabledRules = smartRuleDao.getEnabledRules()
        return enabledRules
            .map { it.toDomain() }
            .firstOrNull { it.matches(text) }
    }

    override suspend fun upsertRule(rule: SmartRule) = smartRuleDao.insert(rule.toEntity())

    override suspend fun deleteRule(rule: SmartRule) = smartRuleDao.delete(rule.toEntity())

    override suspend fun getMaxPriority(): Int = smartRuleDao.getMaxPriority() ?: 0
}
