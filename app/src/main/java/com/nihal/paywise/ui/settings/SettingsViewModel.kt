package com.nihal.paywise.ui.settings

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.UserPreferencesRepository
import com.nihal.paywise.data.repository.BackupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SettingsViewModel(
    private val backupRepository: BackupRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val backupMetadata = userPreferencesRepository.backupMetadata
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun exportCsv(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val csv = backupRepository.getTransactionsCsv()
                val filename = "PayWise_Transactions_${getTimestamp()}.csv"
                saveFile(context, csv, filename, "text/csv", "PayWise")
                userPreferencesRepository.updateLastExport(filename)
                onSuccess(filename)
            } catch (e: Exception) {
                onError(e.message ?: "Export failed")
            }
        }
    }

    fun exportFullBackup(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupRepository.getFullBackupJson()
                val filename = "PayWise_FullBackup_${getTimestamp()}.json"
                saveFile(context, json, filename, "application/json", "PayWise")
                userPreferencesRepository.updateLastExport(filename)
                onSuccess(filename)
            } catch (e: Exception) {
                onError(e.message ?: "Export failed")
            }
        }
    }

    fun importBackup(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                backupRepository.restoreFromJson(jsonString)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Import failed")
            }
        }
    }

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    }

    private fun saveFile(context: Context, content: String, filename: String, mimeType: String, subDir: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/$subDir")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(content.toByteArray())
                }
            }
        } else {
            val dir = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), subDir)
            if (!dir.exists()) dir.mkdirs()
            java.io.File(dir, filename).writeText(content)
        }
    }
}
