package com.nihal.paywise.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.util.MoneyFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardBillDetailScreen(
    onBack: () -> Unit,
    onPayClick: (String, Long) -> Unit,
    viewModel: CardBillDetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(uiState.account?.name ?: "Card Details", fontWeight = FontWeight.Bold) },
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.billInfo?.let { bill ->
                        item {
                            val daysLeft = ChronoUnit.DAYS.between(java.time.LocalDate.now(), bill.dueDate)
                            val isOverdue = daysLeft < 0
                            
                            GlassCard {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("Upcoming Bill", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyFormatter.formatPaise(bill.remainingToPayPaise), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val statusColor = if (isOverdue) MaterialTheme.colorScheme.error else if (daysLeft <= 3) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        Surface(color = statusColor.copy(alpha = 0.1f), shape = CircleShape) {
                                            Text(
                                                text = if (isOverdue) "Overdue" else "Due in $daysLeft days",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Due by ${bill.dueDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    if (bill.remainingToPayPaise > 0) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { onPayClick(uiState.account!!.id, bill.remainingToPayPaise) },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = MaterialTheme.shapes.large
                                        ) {
                                            Icon(Icons.Default.Payment, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Pay Bill Now", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(title = "Cycle Breakdown")
                            GlassCard {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CycleRow("Total Charges", MoneyFormatter.formatPaise(bill.billAmountPaise))
                                    CycleRow("Payments Made", MoneyFormatter.formatPaise(bill.paidAmountPaise), color = Color(0xFF43A047))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    CycleRow("Statement Date", "Last day of cycle")
                                }
                            }
                        }
                    }

                    item { SectionHeader(title = "Recent Card Spends") }

                    if (uiState.transactions.isEmpty()) {
                        item { Text("No recent transactions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(uiState.transactions) { tx ->
                            com.nihal.paywise.ui.home.TransactionRowItem(
                                com.nihal.paywise.ui.home.HomeTransactionUiModel(
                                    id = tx.id,
                                    amountText = MoneyFormatter.formatPaise(tx.amountPaise),
                                    categoryName = "Spending",
                                    accountName = uiState.account?.name ?: "Card",
                                    dateText = tx.timestamp.toString(), // Simplified
                                    timeText = "",
                                    type = tx.type
                                )
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
