package com.nihal.paywise.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.navigation.NotificationNavRequest
import com.nihal.paywise.ui.addtxn.AddTransactionScreen
import com.nihal.paywise.ui.home.HomeScreen
import com.nihal.paywise.ui.onboarding.OnboardingScreen
import com.nihal.paywise.ui.onboarding.OnboardingViewModel
import com.nihal.paywise.ui.recurring.AddRecurringScreen
import com.nihal.paywise.ui.recurring.RecurringHistoryScreen
import com.nihal.paywise.ui.recurring.RecurringListScreen
import com.nihal.paywise.ExpenseTrackerApp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHostState
import com.nihal.paywise.ui.splash.StartRouteScreen
import com.nihal.paywise.ui.budgets.BudgetsScreen
import com.nihal.paywise.ui.reports.ReportsScreen
import com.nihal.paywise.ui.settings.SettingsScreen
import com.nihal.paywise.ui.settings.setpin.SetPinScreen
import com.nihal.paywise.ui.lock.LockScreen

@Composable
fun PayWiseNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onboardingCompleted: Boolean,
    isLocked: Boolean,
    onUnlock: () -> Unit,
    navRequest: NotificationNavRequest? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ExpenseTrackerApp).container

    val startDestination = if (isLocked) "lock" else "start"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("lock") {
            LockScreen(onUnlock = onUnlock)
        }
        composable("start") {
            StartRouteScreen(
                userPreferencesRepository = appContainer.userPreferencesRepository,
                onNavigateToOnboarding = {
                    navController.navigate("onboarding") {
                        popUpTo("lock") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("lock") { inclusive = true }
                    }
                }
            )
        }
        composable("onboarding") {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory)
            OnboardingScreen(
                onFinish = {
                    onboardingViewModel.completeOnboarding()
                    navController.navigate("home") {
                        popUpTo("onboarding") {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onAddClick = { navController.navigate("add_txn") },
                onRecurringClick = { navController.navigate("recurring_list") },
                onBudgetClick = { navController.navigate("budgets") }
            )
        }
        composable("budgets") {
            BudgetsScreen()
        }
        composable("reports") {
            ReportsScreen()
        }
        composable("settings") {
            SettingsScreen(
                snackbarHostState = snackbarHostState,
                onImportSuccess = {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSetPinClick = { navController.navigate("set_pin") }
            )
        }
        composable("set_pin") {
            SetPinScreen(
                onPinSet = { navController.popBackStack() }
            )
        }
        composable("add_txn") {
            AddTransactionScreen(navigateBack = { navController.popBackStack() })
        }
        composable("recurring_list") {
            RecurringListScreen(
                snackbarHostState = snackbarHostState,
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
