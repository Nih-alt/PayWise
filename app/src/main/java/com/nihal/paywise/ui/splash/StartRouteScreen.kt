package com.nihal.paywise.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nihal.paywise.data.local.UserPreferencesRepository
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.PayWiseMarkPlate
import kotlinx.coroutines.flow.first

@Composable
fun StartRouteScreen(
    userPreferencesRepository: UserPreferencesRepository,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(Unit) {
        val completed = userPreferencesRepository.onboardingCompletedFlow.first()
        if (completed) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PayWiseMarkPlate()
        }
    }
}
