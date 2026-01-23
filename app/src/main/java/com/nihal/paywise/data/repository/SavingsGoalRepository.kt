package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.SavingsGoalDao
import com.nihal.paywise.domain.model.SavingsGoal
import com.nihal.paywise.domain.model.toDomain
import com.nihal.paywise.domain.model.toEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

interface SavingsGoalRepository {
    fun getActiveGoalsWithProgress(): Flow<List<SavingsGoal>>
    fun getGoalWithProgress(id: String): Flow<SavingsGoal?>
    suspend fun insertGoal(goal: SavingsGoal)
    suspend fun updateGoal(goal: SavingsGoal)
    suspend fun deleteGoal(goal: SavingsGoal)
}

class OfflineSavingsGoalRepository(
    private val savingsGoalDao: SavingsGoalDao
) : SavingsGoalRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getActiveGoalsWithProgress(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.observeActiveGoals().flatMapLatest { entities ->
            if (entities.isEmpty()) return@flatMapLatest flowOf(emptyList<SavingsGoal>())
            
            val goalFlows = entities.map { entity ->
                savingsGoalDao.observeTotalAllocated(entity.id).map { allocated ->
                    entity.toDomain(allocated ?: 0L)
                }
            }
            combine(goalFlows) { it.toList() }
        }
    }

    override fun getGoalWithProgress(id: String): Flow<SavingsGoal?> {
        return flow {
            val entity = savingsGoalDao.getById(id)
            if (entity != null) {
                emitAll(savingsGoalDao.observeTotalAllocated(id).map { allocated ->
                    entity.toDomain(allocated ?: 0L)
                })
            } else {
                emit(null)
            }
        }
    }

    override suspend fun insertGoal(goal: SavingsGoal) = savingsGoalDao.insert(goal.toEntity())
    override suspend fun updateGoal(goal: SavingsGoal) = savingsGoalDao.update(goal.toEntity())
    override suspend fun deleteGoal(goal: SavingsGoal) = savingsGoalDao.delete(goal.toEntity())
}