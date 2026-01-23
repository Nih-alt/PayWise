package com.nihal.paywise.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["txnId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["txnId"])]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val txnId: String?,
    val claimId: String? = null,
    val storedRelativePath: String,
    val originalFileName: String?,
    val mimeType: String,
    val byteSize: Long,
    val createdAt: Instant = Instant.now()
)