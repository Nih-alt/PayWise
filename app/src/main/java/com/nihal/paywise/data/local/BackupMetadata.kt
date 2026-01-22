package com.nihal.paywise.data.local

data class BackupMetadata(
    val lastBackupTime: Long?,
    val lastBackupFile: String?,
    val lastExportTime: Long?,
    val lastExportFile: String?
)
