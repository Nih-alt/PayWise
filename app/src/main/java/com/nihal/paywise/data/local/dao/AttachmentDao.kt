package com.nihal.paywise.data.local.dao

import androidx.room.*
import com.nihal.paywise.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity)

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE txnId = :txnId")
    fun observeAttachmentsForTxn(txnId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE txnId = :txnId")
    suspend fun getAttachmentsForTxn(txnId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments")
    suspend fun getAllAttachments(): List<AttachmentEntity>
}
