package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.RecurringSkipDao
import com.nihal.paywise.data.local.entity.RecurringSkipEntity
import kotlinx.coroutines.flow.Flow

class OfflineRecurringSkipRepository(
    private val recurringSkipDao: RecurringSkipDao
) : RecurringSkipRepository {

    override suspend fun insertSkip(recurringId: String, yearMonth: String) {
        // Construct composite ID
        val id = "$recurringId|$yearMonth"
        val skip = RecurringSkipEntity(
            id = id,
            recurringId = recurringId,
            yearMonth = yearMonth
        )
        recurringSkipDao.insertSkip(skip)
    }

    override suspend fun deleteSkip(recurringId: String, yearMonth: String) {
        recurringSkipDao.deleteSkip(recurringId, yearMonth)
    }

    override fun getSkipsForYearMonthStream(yearMonth: String): Flow<List<RecurringSkipEntity>> {
        return recurringSkipDao.observeSkipsForYearMonth(yearMonth)
    }

    override suspend fun getSkipsForYearMonth(yearMonth: String): List<RecurringSkipEntity> {
        return recurringSkipDao.getSkipsForYearMonth(yearMonth)
    }

    override suspend fun isSkipped(recurringId: String, yearMonth: String): Boolean {
        return recurringSkipDao.isSkipped(recurringId, yearMonth)
    }
}