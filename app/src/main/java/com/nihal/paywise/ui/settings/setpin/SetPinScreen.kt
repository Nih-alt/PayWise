package com.nihal.paywise.ui.settings.setpin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.R
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.GlassCard
import com.nihal.paywise.ui.components.PayWiseMarkPlate
import kotlinx.coroutines.delay

@Composable
fun SetPinScreen(
    onPinSet: () -> Unit,
    viewModel: SetPinViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            delay(500)
            onPinSet()
        }
    }

    AppBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // Subtle Watermark
            PayWiseMarkPlate(
                modifier = Modifier
                    .size(400.dp)
                    .alpha(0.02f)
                    .offset(y = (-150).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    AnimatedContent(
                        targetState = uiState.isConfirming,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                .togetherWith(fadeOut(animationSpec = tween(90)))
                        },
                        label = "PinTitleAnimation"
                    ) { confirming ->
                        Text(
                            text = if (confirming) "Confirm PIN" else "Set a PIN",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Use 4 digits. You can change it later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                // Glass Card containing the Indicator
                GlassCard(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .wrapContentHeight(),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        PinIndicatorRow(
                            pinLength = uiState.pin.length,
                            hasError = uiState.error != null,
                            isSuccess = uiState.isSuccess
                        )

                        // Error Message with fixed height to prevent jumpy layout
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .padding(top = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.error != null) {
                                Text(
                                    text = uiState.error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Numeric Keypad
                NumericKeypad(
                    onNumberClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.onNumberClick(it)
                    },
                    onBackspace = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onBackspace()
                    },
                    onLongClear = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onClearAll()
                    }
                )
            }
        }
    }
}

@Composable
fun PinIndicatorRow(
    pinLength: Int,
    hasError: Boolean,
    isSuccess: Boolean
) {
    // Shake Animation for Error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(hasError) {
        if (hasError) {
            repeat(4) {
                shakeOffset.animateTo(10f, spring(stiffness = Spring.StiffnessHigh))
                shakeOffset.animateTo(-10f, spring(stiffness = Spring.StiffnessHigh))
            }
            shakeOffset.animateTo(0f)
        }
    }

    Row(
        modifier = Modifier
            .offset(x = shakeOffset.value.dp)
            .semantics { 
                contentDescription = "PIN length: $pinLength of 4" 
            },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            val isFilled = index < pinLength
            val dotColor by animateColorAsState(
                targetValue = when {
                    isSuccess -> Color(0xFF43A047)
                    hasError -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                }, 
                animationSpec = tween(200),
                label = "DotColor"
            )
            
            val borderColor by animateColorAsState(
                targetValue = when {
                    isSuccess -> Color(0xFF43A047)
                    hasError -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
                label = "DotBorderColor"
            )

            val scale by animateFloatAsState(
                targetValue = if (isFilled) 1.25f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ), 
                label = "DotScale"
            )

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = CircleShape
                    )
                    .then(
                        if (isFilled && !hasError && !isSuccess) {
                            Modifier.shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary
                            )
                        } else Modifier
                    )
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onLongClear: () -> Unit
) {
    val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "back")

    Column(
        modifier = Modifier.widthIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (i in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (j in 0 until 3) {
                    val item = numbers[i * 3 + j]
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when (item) {
                            "" -> Spacer(Modifier.size(72.dp))
                            "back" -> KeypadButton(
                                icon = Icons.AutoMirrored.Filled.Backspace,
                                contentDesc = "Backspace",
                                onClick = onBackspace,
                                onLongClick = onLongClear,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            else -> KeypadButton(
                                text = item,
                                contentDesc = "Digit $item",
                                onClick = { onNumberClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
    text: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    contentDesc: String,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ButtonScale"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = if (isPressed) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) 
        else containerColor,
        label = "ButtonColor"
    )

    Surface(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .semantics { contentDescription = contentDesc },
        shape = CircleShape,
        color = animatedColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        shadowElevation = if (isPressed) 0.dp else 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}