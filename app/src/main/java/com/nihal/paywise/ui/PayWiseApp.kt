package com.nihal.paywise.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.nihal.paywise.navigation.PayWiseNavHost
import com.nihal.paywise.ui.theme.PayWiseTheme

@Composable
fun PayWiseApp() {
    PayWiseTheme {
        val navController = rememberNavController()
        PayWiseNavHost(navController = navController)
    }
}