package com.nihal.paywise.ui.lock

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.settings.setpin.PinIndicatorRow
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    viewModel: LockViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Auto-trigger biometric on start if enabled
    LaunchedEffect(uiState.isBiometricEnabled) {
        if (uiState.isBiometricEnabled && !uiState.isSuccess && uiState.cooldownUntil < System.currentTimeMillis()) {
            showBiometricPrompt(context as FragmentActivity, viewModel::onBiometricSuccess)
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onUnlock()
        }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Unlock PayWise",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // PIN Indicator
            PinIndicatorRow(
                pinLength = uiState.pin.length,
                hasError = uiState.error != null,
                isSuccess = uiState.isSuccess
            )

            // Feedback/Cooldown area
            Box(modifier = Modifier.height(60.dp), contentAlignment = Alignment.Center) {
                val now = System.currentTimeMillis()
                if (uiState.cooldownUntil > now) {
                    val remainingTime = remember { mutableLongStateOf((uiState.cooldownUntil - now) / 1000) }
                    LaunchedEffect(remainingTime.longValue) {
                        if (remainingTime.longValue > 0) {
                            delay(1000)
                            remainingTime.longValue -= 1
                        }
                    }
                    Text(
                        text = "Too many attempts. Try again in ${remainingTime.longValue} seconds.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Keypad Grid
            KeypadGrid(
                onNumberClick = viewModel::onNumberClick,
                onBackspace = viewModel::onBackspace,
                onBiometricClick = {
                    showBiometricPrompt(context as FragmentActivity, viewModel::onBiometricSuccess)
                },
                showBiometric = uiState.isBiometricEnabled
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            TextButton(onClick = { /* Explain forgot PIN logic */ }) {
                Text("Forgot PIN?", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun KeypadGrid(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometricClick: () -> Unit,
    showBiometric: Boolean
) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Numbers 1-9
        for (i in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                for (j in 0 until 3) {
                    val num = numbers[i * 3 + j]
                    KeypadButton(text = num, onClick = { onNumberClick(num) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Bottom Row (Biometric, 0, Backspace)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showBiometric) {
                IconButton(
                    onClick = onBiometricClick,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Use biometric", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Spacer(modifier = Modifier.size(72.dp))
            }
            
            KeypadButton(text = "0", onClick = { onNumberClick("0") })
            
            IconButton(
                onClick = onBackspace,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun KeypadButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric login for PayWise")
        .setSubtitle("Log in using your biometric credential")
        .setNegativeButtonText("Use account password")
        .build()

    biometricPrompt.authenticate(promptInfo)
}