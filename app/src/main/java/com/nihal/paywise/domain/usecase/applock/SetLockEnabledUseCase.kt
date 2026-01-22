package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository

class SetLockEnabledUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        appLockRepository.setAppLockEnabled(enabled)
    }
}
