package com.nihal.paywise.ui.settings

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.AppLockSettings
import com.nihal.paywise.data.local.UserPreferencesRepository
import com.nihal.paywise.data.local.BackupMetadata
import com.nihal.paywise.data.repository.BackupRepository
import com.nihal.paywise.domain.model.SalarySettings
import com.nihal.paywise.domain.usecase.applock.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SettingsUiState(
    val language: String = "en",
    val theme: String = "SYSTEM",
    val notificationsEnabled: Boolean = true,
    val salarySettings: SalarySettings = SalarySettings(),
    val appLockSettings: AppLockSettings? = null,
    val backupMetadata: BackupMetadata? = null,
    val appVersion: String = "1.0.0"
)

class SettingsViewModel(
    private val backupRepository: BackupRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getAppLockSettingsUseCase: GetAppLockSettingsUseCase,
    private val setLockEnabledUseCase: SetLockEnabledUseCase,
    private val setPinUseCase: SetPinUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val setAutoLockMinutesUseCase: SetAutoLockMinutesUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.appLanguageFlow,
        userPreferencesRepository.appThemeFlow,
        userPreferencesRepository.notificationsEnabledFlow,
        userPreferencesRepository.salarySettingsFlow,
        getAppLockSettingsUseCase(),
        userPreferencesRepository.backupMetadata
    ) { flows ->
        SettingsUiState(
            language = flows[0] as String,
            theme = flows[1] as String,
            notificationsEnabled = flows[2] as Boolean,
            salarySettings = flows[3] as SalarySettings,
            appLockSettings = flows[4] as AppLockSettings?,
            backupMetadata = flows[5] as BackupMetadata?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setLanguage(lang: String) {
        viewModelScope.launch { userPreferencesRepository.updateAppLanguage(lang) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { userPreferencesRepository.updateAppTheme(theme) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateNotificationsEnabled(enabled) }
    }

    fun updateSalarySettings(settings: SalarySettings) {
        viewModelScope.launch { userPreferencesRepository.updateSalarySettings(settings) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { setLockEnabledUseCase(enabled) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { setBiometricEnabledUseCase(enabled) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch { setAutoLockMinutesUseCase(minutes) }
    }

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