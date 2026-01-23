package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SmartMatchMode {
    CONTAINS, STARTS_WITH, EXACT
}

@Entity(tableName = "smart_rules")
data class SmartRuleEntity(
    @PrimaryKey
    val id: String,
    val matchText: String,
    val matchMode: SmartMatchMode,
    val outputCategoryId: String?,
    val outputTagIds: String?, // Comma separated list
    val outputAccountId: String?,
    val priority: Int,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
