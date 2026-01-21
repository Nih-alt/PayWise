package com.nihal.paywise.ui.recurring

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.SoftCard
import com.nihal.paywise.ui.util.CategoryVisuals
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RecurringListScreen(
    snackbarHostState: SnackbarHostState,
    onAddRecurringClick: () -> Unit,
    onHistoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val recurringItems by viewModel.recurringList.collectAsState()
    val displayMonth by viewModel.displayMonth.collectAsState()
    val itemToConfirm = viewModel.itemToConfirm
    val confirmationType = viewModel.confirmationType
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
                viewModel.refreshCurrentMonth()
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
                            Text("Pay ${itemToConfirm?.title}?")
                            Text("Amount: ${itemToConfirm?.amountText}", fontWeight = FontWeight.Bold)
                            Text("From: ${itemToConfirm?.accountName}")
                            Text("Category: ${itemToConfirm?.categoryName}")
                            Text("For: $displayMonth")
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { itemToConfirm?.let { viewModel.markAsPaid(it.id) } },
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
                         Text("This will stop auto-post and reminders for $displayMonth only.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { itemToConfirm?.let { viewModel.skipThisMonth(it.id) } },
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
        containerColor = Color.Transparent
    ) { paddingValues ->
        AppBackground {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isNotificationPermissionMissing || areSystemNotificationsDisabled) {
                    Box(modifier = Modifier.padding(16.dp)) {
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
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Text("Enable Notifications for Reminders")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        RecurringHeaderCard(
                            recurringItems = recurringItems,
                            displayMonth = displayMonth
                        )
                    }

                    if (recurringItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recurring transactions found.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(recurringItems) { item ->
                            RecurringItemCard(
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringHeaderCard(
    recurringItems: List<RecurringUiModel>,
    displayMonth: String
) {
    val activeCount = recurringItems.count { !it.isPaused && !it.isSkipped && it.status != RecurringDisplayStatus.PAID }
    val skippedCount = recurringItems.count { it.isSkipped }
    val overdueCount = recurringItems.count { it.status == RecurringDisplayStatus.OVERDUE }

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Recurring",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Bills & EMIs",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = displayMonth,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusSummaryChip(
                    label = "Active",
                    count = activeCount,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatusSummaryChip(
                    label = "Skipped",
                    count = skippedCount,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                StatusSummaryChip(
                    label = "Overdue",
                    count = overdueCount,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatusSummaryChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun RecurringItemCard(
    item: RecurringUiModel,
    onPaidClick: () -> Unit,
    onSkipClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onHistoryClick: () -> Unit,
    isSaving: Boolean
) {
    val visual = CategoryVisuals.getVisual(item.categoryName)
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(visual.containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.color
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isPaused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.categoryName} • ${item.accountName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = item.amountText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.dueDateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.status == RecurringDisplayStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Status Chip & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusChip(status = item.status, detail = item.statusDetail)
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleStatus,
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = if (item.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (item.isPaused) "Resume" else "Pause",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onHistoryClick,
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPaidClick,
                    enabled = item.status != RecurringDisplayStatus.PAID && item.status != RecurringDisplayStatus.SKIPPED && !item.isPaused && !isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 0.dp, horizontal = 12.dp)
                ) {
                    Text("Mark Paid")
                }
                
                OutlinedButton(
                    onClick = onSkipClick,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 0.dp, horizontal = 12.dp)
                ) {
                    Text(if (item.isSkipped) "Unskip" else "Skip")
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    status: RecurringDisplayStatus,
    detail: String?
) {
    val (backgroundColor, textColor) = when (status) {
        RecurringDisplayStatus.PAID -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        RecurringDisplayStatus.OVERDUE -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        RecurringDisplayStatus.DUE_TODAY -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        RecurringDisplayStatus.UPCOMING -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        RecurringDisplayStatus.SNOOZED -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        RecurringDisplayStatus.SKIPPED -> Color(0xFFF5F5F5) to Color(0xFF616161)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = detail ?: status.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}