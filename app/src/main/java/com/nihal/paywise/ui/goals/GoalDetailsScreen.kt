package com.nihal.paywise.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.R
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailsScreen(
    onBack: () -> Unit,
    viewModel: GoalDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showAddMoneySheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Goal?") },
            text = { Text("This will delete the goal but keep existing allocations in your account history.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteGoal(onBack) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(uiState.goal?.title ?: "Goal Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.goal != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { GoalHeroCard(uiState.goal!!) }
                    
                    item {
                        Button(
                            onClick = { showAddMoneySheet = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Money to Goal", fontWeight = FontWeight.Bold)
                        }
                    }

                    item { SectionHeader(title = "Allocation History") }

                    if (uiState.allocations.isEmpty()) {
                        item {
                            Text("No allocations yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(uiState.allocations) { tx ->
                            AllocationRow(tx, accounts)
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddMoneySheet) {
        ModalBottomSheet(onDismissRequest = { showAddMoneySheet = false }) {
            AddMoneySheetContent(
                accounts = accounts,
                onConfirm = { accId, amount, note ->
                    viewModel.addAllocation(accId, amount, note)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddMoneySheet = false
                }
            )
        }
    }
}

@Composable
fun GoalHeroCard(goal: SavingsGoal) {
    val color = Color(goal.color)
    val isOverfunded = goal.progressPercent > 1f
    
    GlassCard {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isOverfunded) "Goal Achieved!" else "Total Saved", style = MaterialTheme.typography.titleMedium, color = if (isOverfunded) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(MoneyFormatter.formatPaise(goal.savedAmountPaise), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = if (isOverfunded) Color(0xFF43A047) else color)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = { goal.progressPercent.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(16.dp).clip(CircleShape),
                color = if (isOverfunded) Color(0xFF43A047) else color,
                trackColor = color.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MoneyFormatter.formatPaise(goal.targetAmountPaise), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isOverfunded) "Surplus" else "Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (isOverfunded) MoneyFormatter.formatPaise(goal.savedAmountPaise - goal.targetAmountPaise) 
                               else MoneyFormatter.formatPaise(goal.remainingAmountPaise),
                        fontWeight = FontWeight.Bold,
                        color = if (isOverfunded) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AllocationRow(tx: Transaction, accounts: List<Account>) {
    val account = accounts.find { it.id == tx.accountId }
    GlassCard {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Savings, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(if (account == null) "Deleted Account" else "From ${account.name}", fontWeight = FontWeight.Bold)
                Text(DateTimeFormatterUtil.formatDate(tx.timestamp), style = MaterialTheme.typography.bodySmall)
            }
            Text(MoneyFormatter.formatPaise(tx.amountPaise), fontWeight = FontWeight.Black, color = Color(0xFF43A047))
        }
    }
}

@Composable
fun AddMoneySheetContent(
    accounts: List<Account>,
    onConfirm: (String, Long, String?) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var note by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add Money", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text("Amount (₹)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp)
        )

        Text("From Account", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { acc ->
                FilterChip(
                    selected = selectedAccId == acc.id,
                    onClick = { selectedAccId = acc.id },
                    label = { Text(acc.name) }
                )
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        val paise = amountInput.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
        
        Button(
            onClick = {
                if (paise > 0 && selectedAccId.isNotBlank()) onConfirm(selectedAccId, paise, note.ifBlank { null })
            },
            enabled = paise > 0 && selectedAccId.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Confirm Allocation", fontWeight = FontWeight.Bold)
        }
        Text(
            "Note: This is a transfer to savings and won't count as an expense.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
