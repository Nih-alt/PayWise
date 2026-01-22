package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository

class SetCooldownUntilUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(timestamp: Long) {
        appLockRepository.setCooldownUntil(timestamp)
    }
}
