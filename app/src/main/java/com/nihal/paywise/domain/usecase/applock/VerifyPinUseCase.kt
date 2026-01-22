package com.nihal.paywise.domain.usecase.applock

import com.nihal.paywise.data.local.AppLockRepository
import com.nihal.paywise.util.PinHasher
import kotlinx.coroutines.flow.first

class VerifyPinUseCase(
    private val appLockRepository: AppLockRepository
) {
    suspend operator fun invoke(pin: String): Boolean {
        val storedHash = appLockRepository.pinHashFlow.first()
        return if (storedHash != null) {
            PinHasher.verifyPin(pin, storedHash)
        } else {
            false
        }
    }
}
