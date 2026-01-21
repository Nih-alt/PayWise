package com.nihal.paywise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.nihal.paywise.ui.theme.BackgroundDarkEnd
import com.nihal.paywise.ui.theme.BackgroundDarkStart
import com.nihal.paywise.ui.theme.BackgroundLightEnd
import com.nihal.paywise.ui.theme.BackgroundLightStart

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val startColor = if (isDark) BackgroundDarkStart else BackgroundLightStart
    val endColor = if (isDark) BackgroundDarkEnd else BackgroundLightEnd

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(startColor, endColor)
                )
            )
    ) {
        content()
    }
}
