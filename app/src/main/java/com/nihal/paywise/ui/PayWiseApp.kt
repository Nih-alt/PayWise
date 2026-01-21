package com.nihal.paywise.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.nihal.paywise.navigation.PayWiseNavHost
import com.nihal.paywise.ui.theme.PayWiseTheme

@Composable
fun PayWiseApp(navTarget: String? = null) {
    PayWiseTheme {
        val navController = rememberNavController()
        
        LaunchedEffect(navTarget) {
            if (navTarget == "recurring") {
                navController.navigate("recurring_list")
            }
        }
        
        PayWiseNavHost(navController = navController)
    }
}