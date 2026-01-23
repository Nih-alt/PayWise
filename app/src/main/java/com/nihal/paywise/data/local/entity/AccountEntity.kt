package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nihal.paywise.domain.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: AccountType,
    val openingBalancePaise: Long,
    val statementDay: Int? = null,
    val dueDay: Int? = null,
    val creditLimitPaise: Long? = null
)
