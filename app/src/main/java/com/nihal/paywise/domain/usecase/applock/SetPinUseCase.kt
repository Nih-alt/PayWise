package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository
import com.nihal.paywise.util.PinHasher

class SetPinUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(pin: String) {
        val hash = PinHasher.hashPin(pin)
        appLockRepository.setPinHash(hash)
    }
}
