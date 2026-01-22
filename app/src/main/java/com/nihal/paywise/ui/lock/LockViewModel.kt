package com.nihal.paywise.ui.lock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.AppLockRepository
import com.nihal.paywise.domain.usecase.applock.MarkUnlockedNowUseCase
import com.nihal.paywise.domain.usecase.applock.RegisterFailedAttemptUseCase
import com.nihal.paywise.domain.usecase.applock.ResetFailedAttemptsUseCase
import com.nihal.paywise.domain.usecase.applock.SetCooldownUntilUseCase
import com.nihal.paywise.domain.usecase.applock.VerifyPinUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LockViewModel(
    private val appLockRepository: AppLockRepository,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val markUnlockedNowUseCase: MarkUnlockedNowUseCase,
    private val registerFailedAttemptUseCase: RegisterFailedAttemptUseCase,
    private val resetFailedAttemptsUseCase: ResetFailedAttemptsUseCase,
    private val setCooldownUntilUseCase: SetCooldownUntilUseCase
) : ViewModel() {

    var uiState by mutableStateOf(LockUiState())
        private set

    init {
        viewModelScope.launch {
            appLockRepository.failedAttemptsFlow.collect { attempts ->
                uiState = uiState.copy(failedAttempts = attempts)
                if (attempts >= 5) {
                    val cooldownUntil = System.currentTimeMillis() + 30_000
                    setCooldownUntilUseCase(cooldownUntil)
                    uiState = uiState.copy(cooldownUntil = cooldownUntil)
                }
            }
        }
        viewModelScope.launch {
            appLockRepository.cooldownUntilFlow.collect { cooldownUntil ->
                uiState = uiState.copy(cooldownUntil = cooldownUntil)
            }
        }
    }

    fun onPinChange(pin: String) {
        uiState = uiState.copy(pin = pin, error = null)
        if (pin.length == 4) {
            viewModelScope.launch {
                if (verifyPinUseCase(pin)) {
                    resetFailedAttemptsUseCase()
                    markUnlockedNowUseCase()
                    uiState = uiState.copy(unlocked = true)
                } else {
                    registerFailedAttemptUseCase()
                    uiState = uiState.copy(pin = "", error = "Invalid PIN")
                }
            }
        }
    }

    fun onBiometricClick() {
        uiState = uiState.copy(showBiometricPrompt = true)
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            markUnlockedNowUseCase()
            uiState = uiState.copy(unlocked = true, showBiometricPrompt = false)
        }
    }

    fun onBiometricError() {
        uiState = uiState.copy(showBiometricPrompt = false)
    }
}

data class LockUiState(
    val pin: String = "",
    val error: String? = null,
    val unlocked: Boolean = false,
    val showBiometricPrompt: Boolean = false,
    val failedAttempts: Int = 0,
    val cooldownUntil: Long? = null
)
