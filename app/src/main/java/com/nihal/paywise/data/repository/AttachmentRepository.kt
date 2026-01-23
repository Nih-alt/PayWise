package com.nihal.paywise.data.repository

import com.nihal.paywise.data.local.dao.AttachmentDao
import com.nihal.paywise.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    fun observeAttachmentsForTxn(txnId: String): Flow<List<AttachmentEntity>>
    suspend fun getAttachmentsForTxn(txnId: String): List<AttachmentEntity>
    suspend fun insertAttachment(attachment: AttachmentEntity)
    suspend fun deleteAttachment(attachment: AttachmentEntity)
    suspend fun getAllAttachments(): List<AttachmentEntity>
}

class OfflineAttachmentRepository(
    private val attachmentDao: AttachmentDao
) : AttachmentRepository {
    override fun observeAttachmentsForTxn(txnId: String) = attachmentDao.observeAttachmentsForTxn(txnId)
    override suspend fun getAttachmentsForTxn(txnId: String) = attachmentDao.getAttachmentsForTxn(txnId)
    override suspend fun insertAttachment(attachment: AttachmentEntity) = attachmentDao.insert(attachment)
    override suspend fun deleteAttachment(attachment: AttachmentEntity) = attachmentDao.delete(attachment)
    override suspend fun getAllAttachments() = attachmentDao.getAllAttachments()
}
