package com.nihal.paywise.domain.model

data class AccountWithBalance(
    val account: Account,
    val currentBalancePaise: Long
)