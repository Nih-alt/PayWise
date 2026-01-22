package com.nihal.paywise.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppLockRepository(private val context: Context) {

    private object PreferencesKeys {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val LAST_UNLOCK_TIME = longPreferencesKey("last_unlock_time")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val COOLDOWN_UNTIL = longPreferencesKey("cooldown_until")
    }

    val appLockEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false
        }

    val pinHashFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PIN_HASH]
        }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
        }

    val autoLockMinutesFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_MINUTES] ?: 1
        }

    val lastUnlockTimeFlow: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_UNLOCK_TIME]
        }
    
    val failedAttemptsFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FAILED_ATTEMPTS] ?: 0
        }

    val cooldownUntilFlow: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.COOLDOWN_UNTIL]
        }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setPinHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_HASH] = hash
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_MINUTES] = minutes
        }
    }

    suspend fun markUnlockedNow() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_UNLOCK_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun registerFailedAttempt() {
        context.dataStore.edit { preferences ->
            val currentAttempts = preferences[PreferencesKeys.FAILED_ATTEMPTS] ?: 0
            preferences[PreferencesKeys.FAILED_ATTEMPTS] = currentAttempts + 1
        }
    }

    suspend fun resetFailedAttempts() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAILED_ATTEMPTS] = 0
        }
    }

    suspend fun setCooldownUntil(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COOLDOWN_UNTIL] = timestamp
        }
    }
}
