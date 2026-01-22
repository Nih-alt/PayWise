package com.nihal.paywise.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.SoftCard
import com.nihal.paywise.util.DateTimeFormatterUtil
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun SettingsScreen(
    onImportSuccess: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val metadata by viewModel.backupMetadata.collectAsState(null)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (content != null) {
                viewModel.importBackup(
                    content,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Restore successful!")
                            onImportSuccess()
                        }
                    },
                    onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                    }
                )
            }
        }
    }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Data & Backup",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Automated Backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "PayWise automatically backs up your data once a week to your Documents folder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            metadata?.lastBackupTime?.let {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Last auto-backup: ${DateTimeFormatterUtil.formatDate(Instant.ofEpochMilli(it))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Manual Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            
                            SettingsActionRow(
                                icon = Icons.Default.FileDownload,
                                label = "Export Transactions (CSV)",
                                onClick = {
                                    viewModel.exportCsv(
                                        context,
                                        onSuccess = { scope.launch { snackbarHostState.showSnackbar("Exported: $it") } },
                                        onError = { scope.launch { snackbarHostState.showSnackbar("Error: $it") } }
                                    )
                                }
                            )

                            SettingsActionRow(
                                icon = Icons.Default.FileUpload,
                                label = "Export Full Backup (JSON)",
                                onClick = {
                                    viewModel.exportFullBackup(
                                        context,
                                        onSuccess = { scope.launch { snackbarHostState.showSnackbar("Backup saved: $it") } },
                                        onError = { scope.launch { snackbarHostState.showSnackbar("Error: $it") } }
                                    )
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            SettingsActionRow(
                                icon = Icons.Default.FileUpload,
                                label = "Import Backup (.json)",
                                onClick = { filePickerLauncher.launch("application/json") }
                            )
                        }
                    }
                }

                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Important: Importing a backup will overwrite all current data. Make sure you have a recent export before proceeding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
