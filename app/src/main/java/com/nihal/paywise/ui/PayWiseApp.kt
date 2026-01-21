package com.nihal.paywise.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.nihal.paywise.navigation.NotificationNavRequest
import com.nihal.paywise.navigation.PayWiseNavHost
import com.nihal.paywise.ui.theme.PayWiseTheme

@Composable
fun PayWiseApp(
    navRequest: NotificationNavRequest? = null,
    onNavRequestHandled: () -> Unit = {}
) {
    PayWiseTheme {
        val navController = rememberNavController()
        
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
        
        PayWiseNavHost(navController = navController)
    }
}