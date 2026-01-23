package com.nihal.paywise.ui.claims

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.data.local.entity.ClaimStatus
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimDetailsScreen(
    onBack: () -> Unit,
    viewModel: ClaimDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val context = LocalContext.current
    
    var showReimburseSheet by remember { mutableStateOf(false) }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(uiState.claim?.title ?: "Claim Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.claim != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ClaimHeroCard(uiState.claim!!, uiState.totalAmountPaise)
                    }

                    item {
                        WorkflowActions(
                            status = uiState.claim!!.status,
                            onUpdate = { viewModel.updateStatus(it) },
                            onReimburse = { showReimburseSheet = true }
                        )
                    }

                    item { SectionHeader(title = "Linked Expenses") }

                    if (uiState.items.isEmpty()) {
                        item { Text("No expenses linked.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(uiState.items) { tx ->
                            com.nihal.paywise.ui.home.TransactionRowItem(
                                com.nihal.paywise.ui.home.HomeTransactionUiModel(
                                    id = tx.id,
                                    amountText = MoneyFormatter.formatPaise(tx.amountPaise),
                                    categoryName = "Office",
                                    accountName = "Expense",
                                    dateText = DateTimeFormatterUtil.formatDate(tx.timestamp),
                                    timeText = "",
                                    type = tx.type
                                )
                            )
                        }
                    }
                    
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showReimburseSheet) {
        ModalBottomSheet(onDismissRequest = { showReimburseSheet = false }) {
            ReimburseSheetContent(
                accounts = accounts,
                suggestedAmount = uiState.totalAmountPaise,
                onConfirm = { accId, amount ->
                    viewModel.markReimbursed(accId, amount)
                    showReimburseSheet = false
                }
            )
        }
    }
}

@Composable
fun ClaimHeroCard(claim: com.nihal.paywise.data.local.entity.ClaimEntity, totalAmount: Long) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StatusBadge(claim.status)
            Spacer(Modifier.height(12.dp))
            Text("Total Claim Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(MoneyFormatter.formatPaise(totalAmount), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            
            if (claim.status == ClaimStatus.REIMBURSED) {
                Spacer(Modifier.height(8.dp))
                Text("Reimbursed on ${claim.reimbursedAt?.let { DateTimeFormatterUtil.formatDate(it) }}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun WorkflowActions(
    status: ClaimStatus,
    onUpdate: (ClaimStatus) -> Unit,
    onReimburse: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when(status) {
            ClaimStatus.DRAFT -> {
                Button(onClick = { onUpdate(ClaimStatus.SUBMITTED) }, modifier = Modifier.weight(1f)) {
                    Text("Submit Claim")
                }
            }
            ClaimStatus.SUBMITTED -> {
                Button(onClick = { onUpdate(ClaimStatus.APPROVED) }, modifier = Modifier.weight(1f)) {
                    Text("Approve")
                }
                OutlinedButton(onClick = { onUpdate(ClaimStatus.REJECTED) }, modifier = Modifier.weight(1f)) {
                    Text("Reject")
                }
            }
            ClaimStatus.APPROVED -> {
                Button(onClick = onReimburse, modifier = Modifier.weight(1f)) {
                    Text("Mark as Reimbursed")
                }
            }
            else -> {
                Text("Workflow Completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ReimburseSheetContent(
    accounts: List<com.nihal.paywise.domain.model.Account>,
    suggestedAmount: Long,
    onConfirm: (String, Long) -> Unit
) {
    var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var amountInput by remember { mutableStateOf((suggestedAmount / 100.0).toString()) }

    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Receive Reimbursement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text("Reimbursed Amount (₹)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Deposit to Account", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { acc ->
                FilterChip(selected = selectedAccId == acc.id, onClick = { selectedAccId = acc.id }, label = { Text(acc.name) })
            }
        }

        Button(
            onClick = {
                val paise = amountInput.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
                if (paise > 0 && selectedAccId.isNotBlank()) onConfirm(selectedAccId, paise)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Confirm & Create Income", fontWeight = FontWeight.Bold)
        }
    }
}
