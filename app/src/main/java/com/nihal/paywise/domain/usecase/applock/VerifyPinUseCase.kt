package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository

class VerifyPinUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        return appLockRepository.verifyPin(pin)
    }
}