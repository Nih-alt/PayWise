package com.nihal.paywise.data.local.dao

import androidx.room.*
import com.nihal.paywise.data.local.entity.SmartRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SmartRuleEntity)

    @Update
    suspend fun update(rule: SmartRuleEntity)

    @Delete
    suspend fun delete(rule: SmartRuleEntity)

    @Query("SELECT * FROM smart_rules ORDER BY priority ASC")
    fun observeAllRules(): Flow<List<SmartRuleEntity>>

    @Query("SELECT * FROM smart_rules WHERE enabled = 1 ORDER BY priority ASC")
    suspend fun getEnabledRules(): List<SmartRuleEntity>

    @Query("SELECT MAX(priority) FROM smart_rules")
    suspend fun getMaxPriority(): Int?
}
