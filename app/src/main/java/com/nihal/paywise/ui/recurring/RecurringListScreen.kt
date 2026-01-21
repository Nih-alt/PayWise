package com.nihal.paywise.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import kotlinx.coroutines.flow.collectLatest
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun RecurringListScreen(
    onAddRecurringClick: () -> Unit,
    onHistoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val recurringItems by viewModel.recurringList.collectAsState()
    val itemToConfirm = viewModel.itemToConfirm
    val confirmationType = viewModel.confirmationType
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isNotificationPermissionMissing by remember { mutableStateOf(false) }
    var areSystemNotificationsDisabled by remember { mutableStateOf(false) }

    fun checkNotificationStatus() {
        val notificationManager = NotificationManagerCompat.from(context)
        val areEnabled = notificationManager.areNotificationsEnabled()
        areSystemNotificationsDisabled = !areEnabled

        if (Build.VERSION.SDK_INT >= 33) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            isNotificationPermissionMissing = !hasPermission
        } else {
            isNotificationPermissionMissing = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { checkNotificationStatus() }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkNotificationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkNotificationStatus()
        viewModel.undoEvent.collectLatest { (txId, recurringId) ->
            val result = snackbarHostState.showSnackbar(
                message = "Marked paid",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoMarkAsPaid(txId, recurringId)
            }
        }
    }

    if (itemToConfirm != null) {
        when (confirmationType) {
            RecurringListViewModel.ConfirmationType.PAID -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissConfirmDialog() },
                    title = { Text("Confirm payment") },
                    text = {
                        Column {
                            Text("Pay ${itemToConfirm.title}?")
                            Text("Amount: ${itemToConfirm.amountText}", fontWeight = FontWeight.Bold)
                            Text("From: ${itemToConfirm.accountName}")
                            Text("Category: ${itemToConfirm.categoryName}")
                            Text("For: ${viewModel.displayMonth}")
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.markAsPaid(itemToConfirm.id) },
                            enabled = !viewModel.isSaving
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissConfirmDialog() },
                            enabled = !viewModel.isSaving
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            RecurringListViewModel.ConfirmationType.SKIP -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissConfirmDialog() },
                    title = { Text("Skip this month?") },
                    text = {
                         Text("This will stop auto-post and reminders for ${viewModel.displayMonth} only.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.skipThisMonth(itemToConfirm.id) },
                            enabled = !viewModel.isSaving
                        ) {
                            Text("Skip")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissConfirmDialog() },
                            enabled = !viewModel.isSaving
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecurringClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (isNotificationPermissionMissing || areSystemNotificationsDisabled) {
                Button(
                    onClick = {
                        if (isNotificationPermissionMissing) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(settingsIntent)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text("Enable Notifications")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recurring",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (recurringItems.isEmpty()) {
                Text("No recurring transactions found.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(recurringItems) { item ->
                        RecurringItemRow(
                            item = item,
                            onPaidClick = { viewModel.showConfirmDialog(item) },
                            onSkipClick = { 
                                if (item.isSkipped) viewModel.unskipThisMonth(item.id)
                                else viewModel.showSkipConfirmDialog(item)
                            },
                            onToggleStatus = { viewModel.toggleStatus(item.id) },
                            onHistoryClick = { onHistoryClick(item.id) },
                            isSaving = viewModel.isSaving
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringItemRow(
    item: RecurringUiModel,
    onPaidClick: () -> Unit,
    onSkipClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onHistoryClick: () -> Unit,
    isSaving: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Line 1: title + amountText
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = if (item.isPaused) Color.Gray else Color.Unspecified
            )
            Text(
                text = item.amountText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // Line 2: categoryName + " • " + accountName
        Text(
            text = "${item.categoryName} • ${item.accountName}",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        // Line 3: "Due: <dueDateText>" + status chip text
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.dueDateText,
                fontSize = 14.sp,
                color = if (item.status == RecurringDisplayStatus.OVERDUE) Color.Red else Color.Gray
            )
            
            Text(
                text = item.status.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when (item.status) {
                    RecurringDisplayStatus.PAID -> Color(0xFF4CAF50)
                    RecurringDisplayStatus.OVERDUE -> Color.Red
                    RecurringDisplayStatus.DUE_TODAY -> Color(0xFFFFA000)
                    RecurringDisplayStatus.UPCOMING -> Color(0xFF2196F3)
                    RecurringDisplayStatus.SKIPPED -> Color.Gray
                }
            )
        }

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPaidClick,
                enabled = item.status != RecurringDisplayStatus.PAID && item.status != RecurringDisplayStatus.SKIPPED && !item.isPaused && !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("Mark Paid")
            }
            
            OutlinedButton(
                onClick = onSkipClick,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (item.isSkipped) "Unskip" else "Skip")
            }
        }
        
        Row(
             modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onToggleStatus,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (item.isPaused) "Resume" else "Pause")
            }
            OutlinedButton(
                onClick = onHistoryClick,
                enabled = !isSaving,
                modifier = Modifier.weight(1f)
            ) {
                Text("History")
            }
        }
    }
}