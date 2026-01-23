package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class ClaimStatus {
    DRAFT, SUBMITTED, APPROVED, REIMBURSED, REJECTED
}

@Entity(tableName = "reimbursement_claims")
data class ClaimEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val status: ClaimStatus,
    val notes: String?,
    val createdAt: Instant = Instant.now(),
    val submittedAt: Instant? = null,
    val approvedAt: Instant? = null,
    val reimbursedAt: Instant? = null,
    val reimbursedAmountPaise: Long? = null,
    val incomeTxnId: String? = null // Links to the auto-generated income transaction
)

@Entity(
    tableName = "claim_items",
    primaryKeys = ["claimId", "txnId"],
    foreignKeys = [
        ForeignKey(
            entity = ClaimEntity::class,
            parentColumns = ["id"],
            childColumns = ["claimId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["txnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["txnId"])]
)
data class ClaimItemEntity(
    val claimId: String,
    val txnId: String,
    val includeAmountPaise: Long
)
