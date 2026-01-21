package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.entity.RecurringSnoozeEntity
import kotlinx.coroutines.flow.Flow

interface RecurringSnoozeRepository {
    suspend fun upsert(snooze: RecurringSnoozeEntity)
    suspend fun delete(recurringId: String, yearMonth: String)
    suspend fun get(recurringId: String, yearMonth: String): RecurringSnoozeEntity?
    fun observeForYearMonth(yearMonth: String): Flow<List<RecurringSnoozeEntity>>
    suspend fun getForYearMonth(yearMonth: String): List<RecurringSnoozeEntity>
}
