package com.nihal.paywise.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nihal.paywise.util.DateTimeFormatterUtil
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayWiseScaffold(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val isMainScreen = currentRoute == "home" || currentRoute == "recurring_list" || currentRoute == "budgets"
    val isOnboardingScreen = currentRoute == "onboarding"

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isOnboardingScreen && currentRoute != "start") {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PayWise",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (currentRoute == "home") {
                                AssistChip(
                                    onClick = { },
                                    label = { Text(DateTimeFormatterUtil.formatYearMonth(YearMonth.now())) },
                                    modifier = Modifier.padding(start = 12.dp),
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    border = null,
                                    shape = MaterialTheme.shapes.extraLarge
                                )
                            }
                            Spacer(Modifier.weight(1f)) // Pushes content to the start
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Navigate to Settings or show logs */ }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isMainScreen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        Triple("home", "Home", Icons.Default.Home),
                        Triple("budgets", "Budgets", Icons.Default.Wallet),
                        Triple("recurring_list", "Recurring", Icons.AutoMirrored.Filled.List),
                        Triple("reports", "Reports", Icons.Default.AccountBalance),
                        Triple("settings", "Profile", Icons.Default.Settings)
                    )

                    items.forEach { (route, label, icon) ->
                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = selected,
                            onClick = {
                                if (route == "home" || route == "recurring_list" || route == "budgets" || route == "reports" || route == "settings") {
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isMainScreen && currentRoute != "budgets",
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (currentRoute == "home") navController.navigate("add_txn")
                        else navController.navigate("add_recurring")
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large,
                    icon = { Icon(Icons.Default.Add, "Add") },
                    text = {
                        Text(if (currentRoute == "home") "Add Expense" else "New Rule")
                    }
                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
