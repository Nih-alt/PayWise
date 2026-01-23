package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository

class MarkUnlockedNowUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke() {
        appLockRepository.markUnlocked()
    }
}