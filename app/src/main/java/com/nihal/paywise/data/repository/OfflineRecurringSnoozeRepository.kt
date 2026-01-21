package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.RecurringSnoozeDao
import com.nihal.paywise.data.local.entity.RecurringSnoozeEntity
import kotlinx.coroutines.flow.Flow

class OfflineRecurringSnoozeRepository(private val recurringSnoozeDao: RecurringSnoozeDao) : RecurringSnoozeRepository {
    override suspend fun upsert(snooze: RecurringSnoozeEntity) = recurringSnoozeDao.upsert(snooze)
    override suspend fun delete(recurringId: String, yearMonth: String) = recurringSnoozeDao.delete(recurringId, yearMonth)
    override suspend fun get(recurringId: String, yearMonth: String): RecurringSnoozeEntity? = recurringSnoozeDao.get(recurringId, yearMonth)
    override fun observeForYearMonth(yearMonth: String): Flow<List<RecurringSnoozeEntity>> = recurringSnoozeDao.observeForYearMonth(yearMonth)
    override suspend fun getForYearMonth(yearMonth: String): List<RecurringSnoozeEntity> = recurringSnoozeDao.getForYearMonth(yearMonth)
}
