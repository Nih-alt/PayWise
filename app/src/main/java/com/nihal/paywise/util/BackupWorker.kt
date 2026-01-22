package com.nihal.paywise.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nihal.paywise.ExpenseTrackerApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("BackupWorker", "Starting automatic periodic backup")
        val app = applicationContext as ExpenseTrackerApp
        val container = app.container
        
        return try {
            val json = container.backupRepository.getFullBackupJson()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val filename = "PayWise_AutoBackup_$timestamp.json"
            
            saveBackupFile(json, filename)
            container.userPreferencesRepository.updateLastBackup(filename)
            
            cleanupOldBackups()
            
            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Backup failed", e)
            Result.retry()
        }
    }

    private fun saveBackupFile(content: String, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/PayWise/Backups")
            }
            val resolver = applicationContext.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(content.toByteArray())
                }
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PayWise/Backups")
            if (!dir.exists()) dir.mkdirs()
            File(dir, filename).writeText(content)
        }
    }

    private fun cleanupOldBackups() {
        val backupsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // MediaStore cleanup is complex, we will focus on File API for legacy or scoped storage internal dirs if needed.
            // For P0, we assume the user manages their Documents, but we can try cleaning via File API if accessible.
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PayWise/Backups")
        } else {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "PayWise/Backups")
        }

        if (backupsDir.exists() && backupsDir.isDirectory) {
            val files = backupsDir.listFiles { f -> f.name.startsWith("PayWise_AutoBackup_") && f.name.endsWith(".json") }
            if (files != null && files.size > 8) {
                files.sortBy { it.lastModified() }
                val toDelete = files.size - 8
                for (i in 0 until toDelete) {
                    files[i].delete()
                }
            }
        }
    }
}
