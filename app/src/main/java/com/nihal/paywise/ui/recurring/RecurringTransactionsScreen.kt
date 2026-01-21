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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
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
            text = { Text("Delete payment of ${itemToDelete.amountText} on ${itemToDelete.dateText}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTransaction(itemToDelete.transactionId) }) {
                    Text("Delete", color = Color.Red)
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
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            header?.let { h ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = h.recurringTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = h.amountText, fontSize = 18.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                        Text(text = "${h.categoryName} • ${h.accountName}", fontSize = 14.sp, color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(text = h.dueRuleText, fontSize = 14.sp)
                        Text(text = h.statusText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = "Total Paid (Last 6m): ${h.totalPaidLast6}", fontSize = 14.sp, color = Color.DarkGray)
                    }
                }
            }

            if (isLoading) {
                Text("Loading...")
            } else if (rows.isEmpty()) {
                Text("No payment history found.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(rows) { row ->
                        HistoryRowItem(
                            item = row,
                            onDeleteClick = { viewModel.confirmDelete(row) }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRowItem(
    item: RecurringHistoryRowUiModel,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.amountText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "${item.dateText} • ${item.timeText}", fontSize = 14.sp)
            Text(text = "${item.categoryName} • ${item.accountName}", fontSize = 12.sp, color = Color.Gray)
            if (!item.note.isNullOrBlank()) {
                Text(text = item.note, fontSize = 12.sp, color = Color.DarkGray)
            }
        }
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
        }
    }
}
