package com.nihal.paywise.ui.settings.setpin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.domain.usecase.applock.SetPinUseCase
import kotlinx.coroutines.launch

class SetPinViewModel(
    private val setPinUseCase: SetPinUseCase
) : ViewModel() {

    var uiState by mutableStateOf(SetPinUiState())
        private set

    fun onPinChange(pin: String) {
        uiState = if (uiState.isConfirming) {
            uiState.copy(confirmPin = pin)
        } else {
            uiState.copy(pin = pin)
        }

        if (uiState.pin.length == 4 && !uiState.isConfirming) {
            uiState = uiState.copy(isConfirming = true)
        }

        if (uiState.isConfirming && uiState.confirmPin.length == 4) {
            if (uiState.pin == uiState.confirmPin) {
                viewModelScope.launch {
                    setPinUseCase(uiState.pin)
                    uiState = uiState.copy(isPinSet = true)
                }
            } else {
                uiState = uiState.copy(error = "PINs do not match", pin = "", confirmPin = "", isConfirming = false)
            }
        }
    }
}

data class SetPinUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val isConfirming: Boolean = false,
    val isPinSet: Boolean = false,
    val error: String? = null
)
