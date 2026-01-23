package com.nihal.paywise.domain.model

import com.nihal.paywise.data.local.entity.AccountEntity

enum class AccountType {
    CASH, BANK, CARD
}

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val openingBalancePaise: Long,
    val statementDay: Int? = null,
    val dueDay: Int? = null,
    val creditLimitPaise: Long? = null
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type,
    openingBalancePaise = openingBalancePaise,
    statementDay = statementDay,
    dueDay = dueDay,
    creditLimitPaise = creditLimitPaise
)

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    openingBalancePaise = openingBalancePaise,
    statementDay = statementDay,
    dueDay = dueDay,
    creditLimitPaise = creditLimitPaise
)