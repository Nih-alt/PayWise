package com.nihal.paywise.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nihal.paywise.domain.model.SalarySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        fun budgetAlert80(month: String) = booleanPreferencesKey("budget_alert_80_$month")
        fun budgetAlert100(month: String) = booleanPreferencesKey("budget_alert_100_$month")
        
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val LAST_BACKUP_FILE = stringPreferencesKey("last_backup_file")
        val LAST_EXPORT_TIME = longPreferencesKey("last_export_time")
        val LAST_EXPORT_FILE = stringPreferencesKey("last_export_file")

        val SALARY_CYCLE_ENABLED = booleanPreferencesKey("salary_cycle_enabled")
        val SALARY_DAY = intPreferencesKey("salary_day")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val salarySettingsFlow: Flow<SalarySettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            SalarySettings(
                isEnabled = prefs[PreferencesKeys.SALARY_CYCLE_ENABLED] ?: false,
                salaryDay = prefs[PreferencesKeys.SALARY_DAY] ?: 1
            )
        }

    suspend fun updateSalarySettings(settings: SalarySettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SALARY_CYCLE_ENABLED] = settings.isEnabled
            prefs[PreferencesKeys.SALARY_DAY] = settings.salaryDay
        }
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[PreferencesKeys.APP_LANGUAGE] ?: "en"
        }

    suspend fun updateAppLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LANGUAGE] = language
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    val appThemeFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.APP_THEME] ?: "SYSTEM" }

    suspend fun updateAppTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.APP_THEME] = theme }
    }

    val backupMetadata: Flow<BackupMetadata> = context.dataStore.data
        .map { prefs ->
            BackupMetadata(
                lastBackupTime = prefs[PreferencesKeys.LAST_BACKUP_TIME],
                lastBackupFile = prefs[PreferencesKeys.LAST_BACKUP_FILE],
                lastExportTime = prefs[PreferencesKeys.LAST_EXPORT_TIME],
                lastExportFile = prefs[PreferencesKeys.LAST_EXPORT_FILE]
            )
        }

    suspend fun updateLastBackup(filename: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_BACKUP_TIME] = System.currentTimeMillis(); it[PreferencesKeys.LAST_BACKUP_FILE] = filename }
    }

    suspend fun updateLastExport(filename: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_EXPORT_TIME] = System.currentTimeMillis(); it[PreferencesKeys.LAST_EXPORT_FILE] = filename }
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun isBudgetAlertFired(month: String, threshold: Int): Boolean {
        val preferences = context.dataStore.data.first()
        return if (threshold == 80) {
            preferences[PreferencesKeys.budgetAlert80(month)] ?: false
        } else {
            preferences[PreferencesKeys.budgetAlert100(month)] ?: false
        }
    }

    suspend fun markBudgetAlertFired(month: String, threshold: Int) {
        context.dataStore.edit { preferences ->
            if (threshold == 80) {
                preferences[PreferencesKeys.budgetAlert80(month)] = true
            } else {
                preferences[PreferencesKeys.budgetAlert100(month)] = true
            }
        }
    }
}