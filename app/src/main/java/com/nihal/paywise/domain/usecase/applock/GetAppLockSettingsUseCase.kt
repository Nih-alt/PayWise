package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetAppLockSettingsUseCase(
    private val appLockRepository: AppLockRepository
) {
    operator fun invoke(): Flow<AppLockSettings> {
        return combine(
            appLockRepository.appLockEnabledFlow,
            appLockRepository.biometricEnabledFlow,
            appLockRepository.autoLockMinutesFlow,
            appLockRepository.pinHashFlow
        ) { lockEnabled, biometricEnabled, autoLockMinutes, pinHash ->
            AppLockSettings(
                isLockEnabled = lockEnabled,
                isBiometricEnabled = biometricEnabled,
                autoLockMinutes = autoLockMinutes,
                hasPin = pinHash != null
            )
        }
    }
}

data class AppLockSettings(
    val isLockEnabled: Boolean,
    val isBiometricEnabled: Boolean,
    val autoLockMinutes: Int,
    val hasPin: Boolean
)
