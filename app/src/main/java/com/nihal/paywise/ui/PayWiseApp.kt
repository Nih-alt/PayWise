package com.nihal.paywise.ui

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.nihal.paywise.navigation.NotificationNavRequest
import com.nihal.paywise.navigation.PayWiseNavHost
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.PayWiseScaffold
import com.nihal.paywise.ui.theme.PayWiseTheme

@Composable
fun PayWiseApp(
    onboardingCompleted: Boolean,
    isLocked: Boolean,
    onUnlock: () -> Unit,
    navRequest: NotificationNavRequest? = null,
    onNavRequestHandled: () -> Unit = {}
) {
    PayWiseTheme {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val fabVisible = remember { mutableStateOf(true) }
        
        LaunchedEffect(navRequest) {
            navRequest?.let { request ->
                if (request.target == "recurring") {
                    val route = if (request.recurringId != null) {
                        "recurring_history/${request.recurringId}"
                    } else {
                        "recurring_list"
                    }
                    
                    Log.d("DeepLink", "Navigating to route: $route")
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
                onNavRequestHandled()
            }
        }
        
        AppBackground {
            PayWiseScaffold(
                navController = navController,
                snackbarHostState = snackbarHostState,
                showFab = fabVisible.value
            ) { innerPadding ->
                PayWiseNavHost(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    navRequest = navRequest,
                    onboardingCompleted = onboardingCompleted,
                    isLocked = isLocked,
                    onUnlock = onUnlock,
                    onFabVisibilityChange = { fabVisible.value = it },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
