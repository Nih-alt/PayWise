package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.entity.RecurringSkipEntity
import kotlinx.coroutines.flow.Flow

interface RecurringSkipRepository {
    suspend fun insertSkip(recurringId: String, yearMonth: String)
    suspend fun deleteSkip(recurringId: String, yearMonth: String)
    fun getSkipsForYearMonthStream(yearMonth: String): Flow<List<RecurringSkipEntity>>
    suspend fun getSkipsForYearMonth(yearMonth: String): List<RecurringSkipEntity>
    suspend fun isSkipped(recurringId: String, yearMonth: String): Boolean
}