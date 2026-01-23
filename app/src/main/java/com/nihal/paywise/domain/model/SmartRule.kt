package com.nihal.paywise.domain.model

import com.nihal.paywise.data.local.entity.SmartMatchMode
import com.nihal.paywise.data.local.entity.SmartRuleEntity

data class SmartRule(
    val id: String,
    val matchText: String,
    val matchMode: SmartMatchMode,
    val outputCategoryId: String?,
    val outputTagIds: List<String>,
    val outputAccountId: String?,
    val priority: Int,
    val enabled: Boolean,
    val createdAt: Long
) {
    /**
     * core matching logic used by the engine.
     */
    fun matches(input: String): Boolean {
        if (input.isBlank()) return false
        return when (matchMode) {
            SmartMatchMode.EXACT -> input.equals(matchText, ignoreCase = true)
            SmartMatchMode.STARTS_WITH -> input.startsWith(matchText, ignoreCase = true)
            SmartMatchMode.CONTAINS -> input.contains(matchText, ignoreCase = true)
        }
    }
}

fun SmartRuleEntity.toDomain(): SmartRule = SmartRule(
    id = id,
    matchText = matchText,
    matchMode = matchMode,
    outputCategoryId = outputCategoryId,
    outputTagIds = outputTagIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
    outputAccountId = outputAccountId,
    priority = priority,
    enabled = enabled,
    createdAt = createdAt
)

fun SmartRule.toEntity(): SmartRuleEntity = SmartRuleEntity(
    id = id,
    matchText = matchText,
    matchMode = matchMode,
    outputCategoryId = outputCategoryId,
    outputTagIds = if (outputTagIds.isEmpty()) null else outputTagIds.joinToString(","),
    outputAccountId = outputAccountId,
    priority = priority,
    enabled = enabled,
    createdAt = createdAt
)
