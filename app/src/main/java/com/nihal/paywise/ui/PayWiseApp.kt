package com.nihal.paywise.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.nihal.paywise.navigation.NotificationNavRequest
import com.nihal.paywise.navigation.PayWiseNavHost
import com.nihal.paywise.ui.theme.PayWiseTheme

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.PayWiseScaffold

@Composable
fun PayWiseApp(
    onboardingCompleted: Boolean,
    navRequest: NotificationNavRequest? = null,
    onNavRequestHandled: () -> Unit = {}
) {
    PayWiseTheme {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        
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
                snackbarHostState = snackbarHostState
            ) { innerPadding ->
                PayWiseNavHost(
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    navRequest = navRequest,
                    onboardingCompleted = onboardingCompleted,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}