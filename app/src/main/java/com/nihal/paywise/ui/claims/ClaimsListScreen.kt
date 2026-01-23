package com.nihal.paywise.ui.claims

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.data.local.entity.ClaimEntity
import com.nihal.paywise.data.local.entity.ClaimStatus
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimsListScreen(
    onClaimClick: (String) -> Unit,
    viewModel: ClaimsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val claims by viewModel.allClaims.collectAsState()
    var selectedStatus by remember { mutableStateOf<ClaimStatus?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredClaims = remember(claims, selectedStatus) {
        if (selectedStatus == null) claims else claims.filter { it.status == selectedStatus }
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Reimbursements", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "New Claim")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                ScrollableTabRow(
                    selectedTabIndex = if (selectedStatus == null) 0 else selectedStatus!!.ordinal + 1,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    Tab(selected = selectedStatus == null, onClick = { selectedStatus = null }) {
                        Text("All", modifier = Modifier.padding(16.dp))
                    }
                    ClaimStatus.entries.forEach { status ->
                        Tab(selected = selectedStatus == status, onClick = { selectedStatus = status }) {
                            Text(status.name.lowercase().replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                if (filteredClaims.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = Icons.Default.Assignment,
                            title = "No claims found",
                            subtitle = "Keep track of office expenses and claims."
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredClaims, key = { it.id }) { claim ->
                            ClaimCard(claim, onClick = { onClaimClick(claim.id) })
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddClaimDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                viewModel.createClaim(title)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ClaimCard(claim: ClaimEntity, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(claim.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(DateTimeFormatterUtil.formatDate(claim.createdAt), style = MaterialTheme.typography.bodySmall)
            }
            StatusBadge(claim.status)
        }
    }
}

@Composable
fun StatusBadge(status: ClaimStatus) {
    val color = when(status) {
        ClaimStatus.DRAFT -> Color.Gray
        ClaimStatus.SUBMITTED -> Color(0xFF2196F3)
        ClaimStatus.APPROVED -> Color(0xFF00BCD4)
        ClaimStatus.REIMBURSED -> Color(0xFF4CAF50)
        ClaimStatus.REJECTED -> Color(0xFFF44336)
    }
    Surface(color = color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AddClaimDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Claim") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Claim Title") },
                placeholder = { Text("e.g. Travel - Jan") }
            )
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onConfirm(title) }) { Text("Create") }
        }
    )
}
