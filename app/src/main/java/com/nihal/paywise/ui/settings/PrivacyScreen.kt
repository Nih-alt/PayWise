package com.nihal.paywise.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nihal.paywise.R
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit
) {
    AppBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.privacy_policy), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            PrivacySection(
                                title = "Privacy First",
                                content = "PayWise is an offline-first application. Your financial data, including transactions, budgets, and recurring rules, is stored locally on your device. We do not have servers, and we do not collect, store, or share your data."
                            )
                            
                            PrivacySection(
                                title = "Data Collection",
                                content = "We do not collect any personal information, location data, or usage analytics. The app does not even require Internet access to function."
                            )

                            PrivacySection(
                                title = "Local Storage",
                                content = "Your data is stored in a secure local database (Room) and encrypted/hashed preferences (DataStore). PINs are hashed using PBKDF2; we never store your actual PIN."
                            )

                            PrivacySection(
                                title = "Permissions",
                                content = "• Notifications: Used to remind you of upcoming bills/EMIs.\n• Biometric: Used for optional App Lock security.\n• Exact Alarms: Used to trigger reminders precisely on time."
                            )

                            PrivacySection(
                                title = "Backups",
                                content = "When you export a backup, a file is created in your device's Documents folder. You are responsible for the safety of these files."
                            )

                            PrivacySection(
                                title = "Contact",
                                content = "For any questions, contact us at: support@paywise.example.com"
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Last Updated: January 2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}