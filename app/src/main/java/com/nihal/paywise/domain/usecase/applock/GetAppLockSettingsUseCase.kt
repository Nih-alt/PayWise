package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository
import com.nihal.paywise.data.local.AppLockSettings
import kotlinx.coroutines.flow.Flow

class GetAppLockSettingsUseCase(
    private val appLockRepository: AppLockRepository
) {
    operator fun invoke(): Flow<AppLockSettings> {
        return appLockRepository.settings
    }
}