package com.nihal.paywise.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.navigation.NotificationNavRequest
import com.nihal.paywise.ui.addtxn.TransactionEditorScreen
import com.nihal.paywise.ui.home.HomeScreen
import com.nihal.paywise.ui.onboarding.OnboardingScreen
import com.nihal.paywise.ui.onboarding.OnboardingViewModel
import com.nihal.paywise.ui.recurring.AddRecurringScreen
import com.nihal.paywise.ui.recurring.RecurringHistoryScreen
import com.nihal.paywise.ui.recurring.RecurringListScreen
import com.nihal.paywise.ui.transactions.TransactionsListScreen
import com.nihal.paywise.ui.goals.GoalsScreen
import com.nihal.paywise.ui.goals.GoalDetailsScreen
import com.nihal.paywise.ExpenseTrackerApp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.nihal.paywise.ui.splash.StartRouteScreen
import com.nihal.paywise.ui.budgets.BudgetsScreen
import com.nihal.paywise.ui.reports.ReportsScreen
import com.nihal.paywise.ui.settings.SettingsScreen
import com.nihal.paywise.ui.settings.PrivacyScreen
import com.nihal.paywise.ui.settings.setpin.SetPinScreen
import com.nihal.paywise.ui.lock.LockScreen

@Composable
fun PayWiseNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onboardingCompleted: Boolean,
    isLocked: Boolean,
    onUnlock: () -> Unit,
    onFabVisibilityChange: (Boolean) -> Unit = {},
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
                        popUpTo("start") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("main") {
                        popUpTo("start") { inclusive = true }
                    }
                }
            )
        }
        composable("onboarding") {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory)
            OnboardingScreen(
                onFinish = {
                    onboardingViewModel.completeOnboarding()
                    navController.navigate("main") {
                        popUpTo("onboarding") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        navigation(startDestination = "home", route = "main") {
            composable("home") {
                HomeScreen(
                    onAddClick = { type -> 
                        when {
                            type.startsWith("CARD_DETAILS_") -> {
                                val id = type.removePrefix("CARD_DETAILS_")
                                navController.navigate("card_details/$id")
                            }
                            type.startsWith("CARD_BILL_") -> {
                                val id = type.removePrefix("CARD_BILL_")
                                navController.navigate("card_bill_detail/$id")
                            }
                            else -> navController.navigate("transaction_add?type=$type")
                        }
                    },
                    onAddSalaryClick = { label -> navController.navigate("transaction_add?type=INCOME&cycleLabel=$label") },
                    onRecurringClick = { navController.navigate("recurring_list") },
                    onBudgetClick = { navController.navigate("budgets") },
                    onGoalsClick = { navController.navigate("goals") },
                    onClaimsClick = { navController.navigate("claims_list") },
                    onFabVisibilityChange = onFabVisibilityChange
                )
            }
            composable("transactions") {
                TransactionsListScreen(
                    onTransactionClick = { id -> navController.navigate("transaction_edit/$id") },
                    onAddClick = { type -> navController.navigate("transaction_add?type=$type") }
                )
            }
            composable("budgets") {
                BudgetsScreen()
            }
            composable("reports") {
                ReportsScreen()
            }
            composable("goals") {
                GoalsScreen(onGoalClick = { id -> navController.navigate("goal_details/$id") })
            }
            composable("claims_list") {
                com.nihal.paywise.ui.claims.ClaimsListScreen(onClaimClick = { id -> navController.navigate("claim_details/$id") })
            }
            composable(
                route = "claim_details/{claimId}",
                arguments = listOf(navArgument("claimId") { type = NavType.StringType })
            ) {
                com.nihal.paywise.ui.claims.ClaimDetailsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "goal_details/{goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.StringType })
            ) {
                GoalDetailsScreen(onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    snackbarHostState = snackbarHostState,
                    onImportSuccess = {
                        navController.navigate("home") {
                            popUpTo("main") { inclusive = true }
                        }
                    },
                    onSetPinClick = { navController.navigate("set_pin") },
                    onPrivacyClick = { navController.navigate("privacy") }
                )
            }
            composable("recurring_list") {
                RecurringListScreen(
                    snackbarHostState = snackbarHostState,
                    onAddRecurringClick = { navController.navigate("add_recurring") },
                    onHistoryClick = { id -> navController.navigate("recurring_history/$id") }
                )
            }
            composable(
                route = "card_bill_detail/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.StringType })
            ) {
                com.nihal.paywise.ui.accounts.CardBillDetailScreen(
                    onBack = { navController.popBackStack() },
                    onPayClick = { id, amount -> 
                        navController.navigate("transaction_add?type=TRANSFER") 
                    }
                )
            }
            composable(
                route = "card_details/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.StringType })
            ) {
                com.nihal.paywise.ui.accounts.CardDetailsScreen(
                    onBack = { navController.popBackStack() },
                    onPayClick = { id -> navController.navigate("transaction_add?type=TRANSFER") }
                )
            }
        }

        composable("set_pin") {
            SetPinScreen(
                onPinSet = { navController.popBackStack() }
            )
        }

        composable(
            route = "transaction_add?type={type}&cycleLabel={cycleLabel}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType; defaultValue = "EXPENSE" },
                navArgument("cycleLabel") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            TransactionEditorScreen(navigateBack = { navController.popBackStack() })
        }

        composable(
            route = "transaction_edit/{transactionId}",
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) {
            TransactionEditorScreen(navigateBack = { navController.popBackStack() })
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
        composable("privacy") {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
    }
}