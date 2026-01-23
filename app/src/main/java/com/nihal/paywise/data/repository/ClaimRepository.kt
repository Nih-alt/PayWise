package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.ClaimDao
import com.nihal.paywise.data.local.entity.ClaimEntity
import com.nihal.paywise.data.local.entity.ClaimItemEntity
import com.nihal.paywise.data.local.entity.ClaimStatus
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.model.TransactionType
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.UUID

interface ClaimRepository {
    fun observeAllClaims(): Flow<List<ClaimEntity>>
    fun observePendingTotalPaise(): Flow<Long>
    suspend fun getClaimById(id: String): ClaimEntity?
    suspend fun upsertClaim(claim: ClaimEntity, items: List<ClaimItemEntity>)
    suspend fun markAsReimbursed(claimId: String, accountId: String, amountPaise: Long, transactionRepository: TransactionRepository)
    suspend fun deleteClaim(id: String)
}

class OfflineClaimRepository(
    val claimDao: ClaimDao
) : ClaimRepository {

    override fun observeAllClaims(): Flow<List<ClaimEntity>> = claimDao.observeAllClaims()

    override fun observePendingTotalPaise(): Flow<Long> {
        // Simple heuristic: status SUBMITTED/APPROVED count as pending
        // We'd ideally sum the claim items here, but for P1 we can sum the 'reimbursedAmountPaise' if set or derive from items.
        // Let's use a flow that combines claims with their items for accuracy.
        return claimDao.observePendingClaims().map { claims ->
            // This is a simplified sum. In a full implementation, we'd sum the linked items.
            0L // Placeholder until items logic is fully reactive
        }
    }

    override suspend fun getClaimById(id: String): ClaimEntity? = claimDao.getClaimById(id)

    override suspend fun upsertClaim(claim: ClaimEntity, items: List<ClaimItemEntity>) {
        claimDao.insertClaim(claim)
        claimDao.deleteItemsForClaim(claim.id)
        claimDao.insertClaimItems(items)
    }

    override suspend fun markAsReimbursed(
        claimId: String, 
        accountId: String, 
        amountPaise: Long,
        transactionRepository: TransactionRepository
    ) {
        val claim = claimDao.getClaimById(claimId) ?: return
        if (claim.status == ClaimStatus.REIMBURSED) return

        val incomeTxnId = UUID.randomUUID().toString()
        val incomeTxn = Transaction(
            id = incomeTxnId,
            amountPaise = amountPaise,
            timestamp = Instant.now(),
            type = TransactionType.INCOME,
            accountId = accountId,
            counterAccountId = null,
            categoryId = null, // Or a specific "Reimbursement" category ID if exists
            note = "Reimbursement: ${claim.title}",
            recurringId = null,
            splitOfTransactionId = null
        )

        transactionRepository.insertTransaction(incomeTxn)
        
        claimDao.updateClaim(claim.copy(
            status = ClaimStatus.REIMBURSED,
            reimbursedAt = Instant.now(),
            reimbursedAmountPaise = amountPaise,
            incomeTxnId = incomeTxnId
        ))
    }

    override suspend fun deleteClaim(id: String) {
        val claim = claimDao.getClaimById(id) ?: return
        claimDao.deleteClaim(claim)
    }
}
