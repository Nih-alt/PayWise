package com.nihal.paywise.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.AppLockRepository
import com.nihal.paywise.domain.usecase.applock.MarkUnlockedNowUseCase
import com.nihal.paywise.domain.usecase.applock.RegisterFailedAttemptUseCase
import com.nihal.paywise.domain.usecase.applock.ResetFailedAttemptsUseCase
import com.nihal.paywise.domain.usecase.applock.SetCooldownUntilUseCase
import com.nihal.paywise.domain.usecase.applock.VerifyPinUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockUiState(
    val pin: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val failedAttempts: Int = 0,
    val cooldownUntil: Long = 0L
)

class LockViewModel(
    private val appLockRepository: AppLockRepository,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val markUnlockedNowUseCase: MarkUnlockedNowUseCase,
    private val registerFailedAttemptUseCase: RegisterFailedAttemptUseCase,
    private val resetFailedAttemptsUseCase: ResetFailedAttemptsUseCase,
    private val setCooldownUntilUseCase: SetCooldownUntilUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = appLockRepository.settings.first()
            val currentSettings = appLockRepository.getAppLockSettings()
            _uiState.update { it.copy(
                isBiometricEnabled = settings.isBiometricEnabled,
                failedAttempts = currentSettings.failedAttempts,
                cooldownUntil = currentSettings.cooldownUntil
            ) }
        }
    }

    fun onNumberClick(number: String) {
        if (_uiState.value.pin.length < 4 && _uiState.value.cooldownUntil < System.currentTimeMillis()) {
            val newPin = _uiState.value.pin + number
            _uiState.update { it.copy(pin = newPin, error = null) }
            
            if (newPin.length == 4) {
                verifyPin(newPin)
            }
        }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            if (verifyPinUseCase(pin)) {
                handleSuccess()
            } else {
                handleFailure()
            }
        }
    }

    private suspend fun handleSuccess() {
        resetFailedAttemptsUseCase()
        _uiState.update { it.copy(isSuccess = true, pin = "****") }
        delay(300)
        markUnlockedNowUseCase()
    }

    private suspend fun handleFailure() {
        registerFailedAttemptUseCase()
        val settings = appLockRepository.getAppLockSettings()
        
        if (settings.failedAttempts >= 5) {
            val cooldown = System.currentTimeMillis() + 30000 // 30 seconds
            setCooldownUntilUseCase(cooldown)
            _uiState.update { it.copy(pin = "", error = null, failedAttempts = 5, cooldownUntil = cooldown) }
        } else {
            _uiState.update { it.copy(pin = "", error = "Invalid PIN") }
        }
    }

    fun onBackspace() {
        if (_uiState.value.pin.isNotEmpty()) {
            _uiState.update { it.copy(pin = _uiState.value.pin.dropLast(1)) }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            handleSuccess()
        }
    }
}