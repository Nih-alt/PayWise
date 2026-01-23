package com.nihal.paywise.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import com.nihal.paywise.util.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppLockSettings(
    val isLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 1,
    val hasPin: Boolean = false,
    val failedAttempts: Int = 0,
    val cooldownUntil: Long = 0L
)

class AppLockRepository(private val context: Context) {

    private object Keys {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val LAST_UNLOCKED_TIME = longPreferencesKey("last_unlocked_time")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val COOLDOWN_UNTIL = longPreferencesKey("cooldown_until")
    }

    val settings: Flow<AppLockSettings> = context.dataStore.data.map { prefs ->
        AppLockSettings(
            isLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            isBiometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            autoLockMinutes = prefs[Keys.AUTO_LOCK_MINUTES] ?: 1,
            hasPin = !prefs[Keys.PIN_HASH].isNullOrEmpty(),
            failedAttempts = prefs[Keys.FAILED_ATTEMPTS] ?: 0,
            cooldownUntil = prefs[Keys.COOLDOWN_UNTIL] ?: 0L
        )
    }

    suspend fun getAppLockSettings(): AppLockSettings = settings.first()

    suspend fun setLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun setPin(pin: String) {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hashPin(pin, salt)
        context.dataStore.edit {
            it[Keys.PIN_SALT] = salt
            it[Keys.PIN_HASH] = hash
            it[Keys.APP_LOCK_ENABLED] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[Keys.PIN_SALT] ?: return false
        val hash = prefs[Keys.PIN_HASH] ?: return false
        return PinHasher.verifyPin(pin, salt, hash)
    }

    suspend fun markUnlocked() {
        context.dataStore.edit { it[Keys.LAST_UNLOCKED_TIME] = System.currentTimeMillis() }
    }

    suspend fun getLastUnlockedTime(): Long {
        return context.dataStore.data.first()[Keys.LAST_UNLOCKED_TIME] ?: 0L
    }

    suspend fun isLockEnabled(): Boolean {
        return context.dataStore.data.first()[Keys.APP_LOCK_ENABLED] ?: false
    }

    suspend fun registerFailedAttempt() {
        context.dataStore.edit {
            val current = it[Keys.FAILED_ATTEMPTS] ?: 0
            it[Keys.FAILED_ATTEMPTS] = current + 1
        }
    }

    suspend fun resetFailedAttempts() {
        context.dataStore.edit {
            it[Keys.FAILED_ATTEMPTS] = 0
            it[Keys.COOLDOWN_UNTIL] = 0L
        }
    }

    suspend fun setCooldownUntil(time: Long) {
        context.dataStore.edit { it[Keys.COOLDOWN_UNTIL] = time }
    }
}
