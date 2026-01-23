package com.nihal.paywise.data.local.dao

import androidx.room.*
import com.nihal.paywise.data.local.entity.ClaimEntity
import com.nihal.paywise.data.local.entity.ClaimItemEntity
import com.nihal.paywise.data.local.entity.ClaimStatus
import com.nihal.paywise.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaimDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaim(claim: ClaimEntity)

    @Update
    suspend fun updateClaim(claim: ClaimEntity)

    @Delete
    suspend fun deleteClaim(claim: ClaimEntity)

    @Query("SELECT * FROM reimbursement_claims ORDER BY createdAt DESC")
    fun observeAllClaims(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM reimbursement_claims WHERE id = :id")
    suspend fun getClaimById(id: String): ClaimEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaimItems(items: List<ClaimItemEntity>)

    @Query("DELETE FROM claim_items WHERE claimId = :claimId")
    suspend fun deleteItemsForClaim(claimId: String)

    @Query("SELECT * FROM claim_items WHERE claimId = :claimId")
    fun observeItemsForClaim(claimId: String): Flow<List<ClaimItemEntity>>

    @Query("SELECT * FROM reimbursement_claims WHERE status IN ('SUBMITTED', 'APPROVED')")
    fun observePendingClaims(): Flow<List<ClaimEntity>>
}
