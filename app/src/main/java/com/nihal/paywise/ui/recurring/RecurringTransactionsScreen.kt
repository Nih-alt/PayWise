package com.nihal.paywise.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.AppBackground
import androidx.compose.foundation.ExperimentalFoundationApi
import com.nihal.paywise.ui.components.EmptyState
import com.nihal.paywise.ui.components.ShimmerLoadingListItem
import com.nihal.paywise.ui.components.SoftCard
import com.nihal.paywise.ui.util.CategoryVisuals
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecurringHistoryScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringHistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val header by viewModel.header.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val itemToDelete = viewModel.itemToDelete
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.undoDeleteEvent.collectLatest {
            val result = snackbarHostState.showSnackbar(
                message = "Payment deleted",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            title = { Text("Delete payment?") },
            text = { 
                Column {
                    Text("Are you sure you want to delete this payment record?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${itemToDelete?.amountText} on ${itemToDelete?.dateText}",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { itemToDelete?.let { viewModel.deleteTransaction(it.transactionId) } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp)
            ) {
                IconButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { innerPadding ->
        AppBackground {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                header?.let { h ->
                    Spacer(modifier = Modifier.height(16.dp))
                    HistoryHeaderCard(header = h)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (isLoading) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(3) { // Show 3 shimmer items as a placeholder
                            ShimmerLoadingListItem()
                        }
                    }
                } else if (rows.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No history yet",
                        subtitle = "Payments recorded for this rule will appear in a timeline here.",
                        hint = "Mark a payment as 'Paid' to see it here"
                    )
                } else {
                    // Group rows by Month Year
                    val groupedRows = remember(rows) {
                        rows.groupBy { row ->
                            // Extract "MMM yyyy" from "dd MMM yyyy"
                            // Assumes dateText is "dd MMM yyyy"
                            val parts = row.dateText.split(" ")
                            if (parts.size >= 3) {
                                "${parts[1]} ${parts[2]}"
                            } else {
                                "Unknown Date"
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedRows.forEach { (month, items) ->
                            item(key = month) { // Add key for animateItemPlacement
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .animateItemPlacement() // Animate the header
                                )
                            }
                            
                            items(items, key = { it.transactionId }) { row ->
                                TimelineRowItem(
                                    item = row,
                                    onDeleteClick = { viewModel.confirmDelete(row) },
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryHeaderCard(header: RecurringHistoryHeaderUiModel) {
    val visual = CategoryVisuals.getVisual(header.categoryName)
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(visual.containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = header.recurringTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${header.categoryName} • ${header.accountName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = header.amountText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoColumn(label = "Due Rule", value = header.dueRuleText)
                InfoColumn(
                    label = "Status", 
                    value = header.statusText, 
                    valueColor = if (header.statusText.contains("Not paid", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface 
                )
            }
             Spacer(modifier = Modifier.height(12.dp))
             InfoColumn(label = "Total Paid (Last 6m)", value = header.totalPaidLast6)
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun TimelineRowItem(
    item: RecurringHistoryRowUiModel,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = CategoryVisuals.getVisual(item.categoryName)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            // Top Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp) // Adjust based on alignment
                    .background(visual.color.copy(alpha = 0.3f))
            )
            // Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(visual.color)
            )
            // Bottom Line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight() // This might need fixed height or weight if inside a fixed row
                    .weight(1f, fill = false) 
                    .background(visual.color.copy(alpha = 0.3f))
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(visual.containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.amountText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.dateText} • ${item.timeText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                         if (!item.note.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                    
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}