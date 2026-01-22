package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository

class RegisterFailedAttemptUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke() {
        appLockRepository.registerFailedAttempt()
    }
}
