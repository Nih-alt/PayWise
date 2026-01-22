package com.nihal.paywise.ui.lock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.ui.draw.alpha
import com.nihal.paywise.ui.components.SoftCard
import com.nihal.paywise.ui.theme.PayWiseTheme
import java.util.concurrent.Executors

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    viewModel: LockViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState

    if (uiState.unlocked) {
        onUnlock()
    }

    PayWiseTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SoftCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Unlock PayWise", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    PinDots(pin = uiState.pin)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    uiState.error?.let { err ->
                         Text(err, color = MaterialTheme.colorScheme.error)
                    }

                    val remainingTime = remember { mutableLongStateOf(0L) }
                    val cooldown = uiState.cooldownUntil
                    if (cooldown != null && cooldown > System.currentTimeMillis()) {
                        LaunchedEffect(cooldown) {
                            while (System.currentTimeMillis() < cooldown) {
                                remainingTime.longValue = (cooldown - System.currentTimeMillis()) / 1000
                                delay(1000)
                            }
                            remainingTime.longValue = 0
                        }
                        Text("Too many attempts. Try again in ${remainingTime.longValue} seconds.")
                    }
                    PinKeypad(
                        enabled = (cooldown == null || System.currentTimeMillis() > cooldown),
                        onPinChange = viewModel::onPinChange,
                        pin = uiState.pin
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BiometricButton(
                        onClick = viewModel::onBiometricClick
                    )
                }
            }
        }
    }

    if (uiState.showBiometricPrompt) {
        BiometricPrompt(
            onSuccess = viewModel::onBiometricSuccess,
            onError = viewModel::onBiometricError
        )
    }
}

@Composable
fun PinDots(pin: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 0 until 4) {
            val alpha by animateFloatAsState(if (i < pin.length) 1f else 0.3f, label = "")
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .alpha(alpha)
            )
        }
    }
}

@Composable
fun PinKeypad(
    enabled: Boolean,
    onPinChange: (String) -> Unit,
    pin: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PinButton(enabled, "1", onPinChange, pin)
            PinButton(enabled, "2", onPinChange, pin)
            PinButton(enabled, "3", onPinChange, pin)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PinButton(enabled, "4", onPinChange, pin)
            PinButton(enabled, "5", onPinChange, pin)
            PinButton(enabled, "6", onPinChange, pin)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PinButton(enabled, "7", onPinChange, pin)
            PinButton(enabled, "8", onPinChange, pin)
            PinButton(enabled, "9", onPinChange, pin)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.width(72.dp))
            PinButton(enabled, "0", onPinChange, pin)
            IconButton(
                enabled = enabled && pin.isNotEmpty(),
                onClick = { onPinChange(pin.dropLast(1)) }
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Backspace")
            }
        }
    }
}

@Composable
fun PinButton(
    enabled: Boolean,
    number: String,
    onPinChange: (String) -> Unit,
    pin: String
) {
    Button(
        enabled = enabled,
        onClick = { if (pin.length < 4) onPinChange(pin + number) },
        modifier = Modifier.size(72.dp),
        shape = CircleShape
    ) {
        Text(number, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun BiometricButton(
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val biometricManager = BiometricManager.from(context)
    val canAuth =
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    if (canAuth) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.Fingerprint, contentDescription = "Use fingerprint")
        }
    }
}

@Composable
fun BiometricPrompt(
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val executor = Executors.newSingleThreadExecutor()

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric login for PayWise")
        .setSubtitle("Log in using your biometric credential")
        .setNegativeButtonText("Use account password")
        .build()

    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                onError()
            }
        })

    biometricPrompt.authenticate(promptInfo)
}
