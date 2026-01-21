package com.nihal.paywise.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nihal.paywise.ui.addtxn.AddTransactionScreen
import com.nihal.paywise.ui.home.HomeScreen
import com.nihal.paywise.ui.recurring.AddRecurringScreen
import com.nihal.paywise.ui.recurring.RecurringHistoryScreen
import com.nihal.paywise.ui.recurring.RecurringListScreen

@Composable
fun PayWiseNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onAddClick = { navController.navigate("add_txn") },
                onRecurringClick = { navController.navigate("recurring_list") }
            )
        }
        composable("add_txn") {
            AddTransactionScreen(navigateBack = { navController.popBackStack() })
        }
        composable("recurring_list") {
            RecurringListScreen(
                onAddRecurringClick = { navController.navigate("add_recurring") },
                onHistoryClick = { id -> navController.navigate("recurring_history/$id") }
            )
        }
        composable(
            route = "recurring_history/{recurringId}",
            arguments = listOf(navArgument("recurringId") { type = NavType.StringType })
        ) {
            RecurringHistoryScreen(navigateBack = { navController.popBackStack() })
        }
        composable("add_recurring") {
            AddRecurringScreen(navigateBack = { navController.popBackStack() })
        }
    }
}