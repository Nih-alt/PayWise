package com.nihal.paywise.ui.settings.setpin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.lock.PinDots
import com.nihal.paywise.ui.lock.PinKeypad

@Composable
fun SetPinScreen(
    onPinSet: () -> Unit,
    viewModel: SetPinViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by remember { mutableStateOf(viewModel.uiState) }

    if (uiState.isPinSet) {
        LaunchedEffect(Unit) {
            onPinSet()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (uiState.isConfirming) "Confirm PIN" else "Set a PIN",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        PinDots(pin = if (uiState.isConfirming) uiState.confirmPin else uiState.pin)
        Spacer(modifier = Modifier.height(16.dp))
        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        PinKeypad(
            enabled = true,
            onPinChange = viewModel::onPinChange,
            pin = if (uiState.isConfirming) uiState.confirmPin else uiState.pin
        )
    }
}
