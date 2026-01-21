package com.nihal.paywise.data.repository

import com.nihal.paywise.domain.model.Recurring
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun getAllRecurringStream(): Flow<List<Recurring>>
    suspend fun getRecurringById(id: String): Recurring?
    suspend fun getActiveRecurring(): List<Recurring>
    suspend fun insertRecurring(recurring: Recurring)
    suspend fun updateRecurring(recurring: Recurring)
    suspend fun deleteRecurring(recurring: Recurring)
}
