package com.nihal.paywise.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.ui.util.CategoryVisuals
import com.nihal.paywise.util.DateTimeFormatterUtil
import com.nihal.paywise.util.MoneyFormatter
import java.time.YearMonth

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf<BudgetEditTarget?>(null) }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            BudgetsHeader(
                currentMonth = uiState.currentMonth,
                onPrev = { viewModel.goPrevMonth() },
                onNext = { viewModel.goNextMonth() }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Overall Budget Card
                item {
                    OverallBudgetCard(
                        status = uiState.overallStatus,
                        onEdit = {
                            showEditDialog = BudgetEditTarget(
                                title = "Monthly Budget",
                                currentAmountPaise = uiState.overallStatus?.budgetPaise ?: 0L,
                                categoryId = null
                            )
                        }
                    )
                }

                // 2. Category Budgets Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Budgets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        TextButton(onClick = { viewModel.copyPreviousMonthBudgets() }) {
                            Text("Copy Last Month", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // 3. Category List
                if (uiState.categoryStatuses.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Wallet,
                            title = "No categories",
                            subtitle = "Add expense categories to set budgets."
                        )
                    }
                } else {
                    items(uiState.categoryStatuses, key = { it.category.id }) { item ->
                        CategoryBudgetRow(
                            item = item,
                            onEdit = {
                                showEditDialog = BudgetEditTarget(
                                    title = item.category.name,
                                    currentAmountPaise = item.status?.budgetPaise ?: 0L,
                                    categoryId = item.category.id
                                )
                            }
                        )
                    }
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showEditDialog != null) {
        BudgetEditDialog(
            target = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onSave = { amountPaise ->
                if (showEditDialog!!.categoryId == null) {
                    viewModel.setOverallBudget(amountPaise)
                } else {
                    viewModel.setCategoryBudget(showEditDialog!!.categoryId!!, amountPaise)
                }
                showEditDialog = null
            }
        )
    }
}

@Composable
fun BudgetsHeader(
    currentMonth: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Budgets",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = CircleShape
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev")
                }
                Text(
                    text = DateTimeFormatterUtil.formatYearMonth(currentMonth),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 80.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next")
                }
            }
        }
    }
}

@Composable
fun OverallBudgetCard(
    status: com.nihal.paywise.domain.model.BudgetStatus?,
    onEdit: () -> Unit
) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "Monthly Budget",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = status?.let { MoneyFormatter.formatPaise(it.budgetPaise) } ?: "Not set",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val progress = status?.percentUsed ?: 0f
            val color = when {
                progress > 1f -> MaterialTheme.colorScheme.error
                progress > 0.8f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }

            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BudgetStat(
                    label = "Spent",
                    value = MoneyFormatter.formatPaise(status?.spentPaise ?: 0L)
                )
                BudgetStat(
                    label = "Remaining",
                    value = MoneyFormatter.formatPaise(status?.remainingPaise ?: 0L),
                    alignment = Alignment.End,
                    valueColor = if ((status?.remainingPaise ?: 0L) < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CategoryBudgetRow(
    item: CategoryBudgetUiModel,
    onEdit: () -> Unit
) {
    val visual = CategoryVisuals.getVisual(item.category.name)
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(visual.containerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(visual.icon, null, modifier = Modifier.size(20.dp), tint = visual.color)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (item.status != null) "Budget: ${MoneyFormatter.formatPaise(item.status.budgetPaise)}" else "No budget set",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                }
            }

            if (item.status != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { item.status.percentUsed.coerceAtMost(1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = visual.color,
                    trackColor = visual.containerColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "${(item.status.percentUsed * 100).toInt()}% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.status.percentUsed > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetStat(
    label: String,
    value: String,
    alignment: Alignment.Horizontal = Alignment.Start,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = alignment) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

data class BudgetEditTarget(
    val title: String,
    val currentAmountPaise: Long,
    val categoryId: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditDialog(
    target: BudgetEditTarget,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var amountInput by remember { mutableStateOf(if (target.currentAmountPaise > 0) (target.currentAmountPaise / 100.0).toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set budget for ${target.title}") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("₹") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paise = amountInput.toDoubleOrNull()?.let { (it * 100).toLong() } ?: 0L
                    onSave(paise)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
