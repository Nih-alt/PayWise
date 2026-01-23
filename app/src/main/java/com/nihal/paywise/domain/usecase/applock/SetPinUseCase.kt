package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository

class SetPinUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(pin: String) {
        appLockRepository.setPin(pin)
    }
}