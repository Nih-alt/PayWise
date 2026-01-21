package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.RecurringDao
import com.nihal.paywise.domain.model.Recurring
import com.nihal.paywise.domain.model.toDomain
import com.nihal.paywise.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineRecurringRepository(private val recurringDao: RecurringDao) : RecurringRepository {
    override fun getAllRecurringStream(): Flow<List<Recurring>> =
        recurringDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getRecurringById(id: String): Recurring? =
        recurringDao.getById(id)?.toDomain()

    override suspend fun getActiveRecurring(): List<Recurring> =
        recurringDao.getActive().map { it.toDomain() }

    override suspend fun insertRecurring(recurring: Recurring) =
        recurringDao.insert(recurring.toEntity())

    override suspend fun updateRecurring(recurring: Recurring) =
        recurringDao.update(recurring.toEntity())

    override suspend fun deleteRecurring(recurring: Recurring) =
        recurringDao.delete(recurring.toEntity())
}
