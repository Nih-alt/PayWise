package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository

class SetBiometricEnabledUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        appLockRepository.setBiometricEnabled(enabled)
    }
}
