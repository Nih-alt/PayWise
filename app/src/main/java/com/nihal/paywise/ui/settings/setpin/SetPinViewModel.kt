package com.nihal.paywise.ui.settings.setpin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.domain.usecase.applock.SetPinUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SetPinUiState(
    val pin: String = "",
    val firstPin: String = "",
    val isConfirming: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class SetPinViewModel(
    private val setPinUseCase: SetPinUseCase
) : ViewModel() {

    var uiState by mutableStateOf(SetPinUiState())
        private set

    fun onNumberClick(number: String) {
        if (uiState.pin.length < 4 && !uiState.isSuccess) {
            val newPin = uiState.pin + number
            uiState = uiState.copy(pin = newPin, error = null)
            
            if (newPin.length == 4) {
                handlePinComplete(newPin)
            }
        }
    }

    private fun handlePinComplete(completedPin: String) {
        viewModelScope.launch {
            delay(300) // Aesthetic delay for user to see the last dot fill
            if (!uiState.isConfirming) {
                // First step complete
                uiState = uiState.copy(
                    firstPin = completedPin,
                    pin = "",
                    isConfirming = true
                )
            } else {
                // Confirmation step
                if (completedPin == uiState.firstPin) {
                    uiState = uiState.copy(isSuccess = true)
                    setPinUseCase(completedPin)
                } else {
                    // Mismatch - trigger shake in UI
                    uiState = uiState.copy(error = "PINs do not match")
                    delay(500)
                    uiState = uiState.copy(pin = "", error = null)
                }
            }
        }
    }

    fun onBackspace() {
        if (uiState.pin.isNotEmpty()) {
            uiState = uiState.copy(pin = uiState.pin.dropLast(1), error = null)
        }
    }

    fun onClearAll() {
        uiState = uiState.copy(pin = "", error = null)
    }
}