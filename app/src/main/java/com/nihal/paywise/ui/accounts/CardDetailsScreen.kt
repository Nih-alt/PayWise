package com.nihal.paywise.ui.accounts

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailsScreen(
    onBack: () -> Unit,
    onPayClick: (String) -> Unit,
    viewModel: CardDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Card
                    uiState.statement?.let { statement ->
                        item {
                            CardSummaryGlass(statement, onPayClick)
                        }
                        
                        item {
                            SectionHeader(title = "Cycle Breakdown")
                            GlassCard {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CycleRow("Statement Date", "${statement.statementPeriodEnd.atZone(java.time.ZoneId.systemDefault()).dayOfMonth}")
                                    CycleRow("Due Date", statement.dueDate.toString())
                                    Divider()
                                    CycleRow("This Cycle Spends", MoneyFormatter.formatPaise(statement.totalChargesPaise))
                                    CycleRow("Payments Received", MoneyFormatter.formatPaise(statement.totalPaymentsPaise), color = Color(0xFF43A047))
                                }
                            }
                        }
                    }

                    item {
                        SectionHeader(title = "Transactions in Cycle")
                    }

                    if (uiState.recentTransactions.isEmpty()) {
                        item {
                            Text("No transactions in this statement period.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(uiState.recentTransactions) { tx ->
                            // Reuse existing row if possible or create simple one
                            TransactionRowSimple(tx)
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun CardSummaryGlass(statement: CardStatement, onPayClick: (String) -> Unit) {
    val statusColor = when(statement.status) {
        CardPaymentStatus.PAID -> Color(0xFF43A047)
        CardPaymentStatus.OVERDUE -> MaterialTheme.colorScheme.error
        CardPaymentStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Due", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(MoneyFormatter.formatPaise(statement.netDuePaise), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                Surface(color = statusColor.copy(alpha = 0.1f), shape = CircleShape) {
                    Text(
                        statement.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Due Date: ${statement.dueDate}", style = MaterialTheme.typography.bodySmall)
            }

            if (statement.netDuePaise > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onPayClick(statement.accountId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Pay Now")
                }
            }
        }
    }
}

@Composable
fun CycleRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun TransactionRowSimple(tx: Transaction) {
    // Simple implementation for card list
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.note ?: "Card Spend", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(DateTimeFormatterUtil.formatDate(tx.timestamp), style = MaterialTheme.typography.bodySmall)
        }
        Text(MoneyFormatter.formatPaise(tx.amountPaise), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
