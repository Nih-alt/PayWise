package com.nihal.paywise.data.repository

interface BackupRepository {
    suspend fun getFullBackupJson(): String
    suspend fun restoreFromJson(jsonString: String)
    suspend fun getTransactionsCsv(): String
}
