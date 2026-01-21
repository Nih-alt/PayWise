package com.nihal.paywise.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.domain.model.TransactionType
import com.nihal.paywise.ui.components.*
import java.time.YearMonth
import com.nihal.paywise.util.DateTimeFormatterUtil

import com.nihal.paywise.ui.util.CategoryVisuals

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onRecurringClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val categories by viewModel.categorySummary.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Hero Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HeroCard(
                month = DateTimeFormatterUtil.formatYearMonth(YearMonth.now()),
                totalSpent = summary.totalExpense
            )
        }

        // 2. Quick Actions
        item {
            SectionHeader(title = "Quick Actions")
            QuickActionsRow(
                onAddExpense = onAddClick,
                onRecurring = onRecurringClick
            )
        }

        // 3. Category Summary
        if (categories.isNotEmpty()) {
            item {
                SectionHeader(title = "Top Spending")
                CategorySummaryRow(categories)
            }
        }

        // 4. Recent Transactions
        item {
            SectionHeader(
                title = "Recent Activity",
                actionText = "See all",
                onActionClick = { /* Navigate to all transactions */ }
            )
        }

        if (transactions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Info,
                    title = "No transactions",
                    subtitle = "Tap 'Add Expense' to get started."
                )
            }
        } else {
            item {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    transactions.take(5).forEachIndexed { index, tx ->
                        TransactionRowItem(tx)
                        if (index < transactions.take(5).size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroCard(month: String, totalSpent: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = month,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "On Track",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            AmountText(
                amount = totalSpent,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text("₹ --", style = MaterialTheme.typography.titleSmall)
                }
                Column {
                    Text(
                        "Savings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text("₹ --", style = MaterialTheme.typography.titleSmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text("80%", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(onAddExpense: () -> Unit, onRecurring: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            ActionChip(
                icon = Icons.Default.Add,
                label = "Expense",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onAddExpense
            )
        }
        item {
            ActionChip(
                icon = Icons.Default.KeyboardArrowDown, // Income usually goes down (or up depending on mental model), arrow down often means 'in' to wallet
                label = "Income",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                badge = "Soon",
                enabled = false,
                onClick = {}
            )
        }
        item {
            ActionChip(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Transfer",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                enabled = false,
                onClick = {}
            )
        }
        item {
            ActionChip(
                icon = Icons.Default.Refresh,
                label = "Recurring",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onRecurring
            )
        }
    }
}

@Composable
fun ActionChip(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    badge: String? = null,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f),
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            if (badge != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    modifier = Modifier
                        .background(contentColor.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CategorySummaryRow(categories: List<CategorySummaryUiModel>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { category ->
            val visual = CategoryVisuals.getVisual(category.name)
            SoftCard(
                modifier = Modifier.weight(1f),
                elevation = 1.dp
            ) {
                Text(category.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${category.totalAmount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { category.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = visual.color,
                    trackColor = visual.containerColor
                )
            }
        }
    }
}

@Composable
fun TransactionRowItem(transaction: HomeTransactionUiModel) {
    val visual = CategoryVisuals.getVisual(transaction.categoryName)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = visual.containerColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = visual.color
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.categoryName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${transaction.dateText} • ${transaction.accountName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Text(
            text = (if (transaction.type == TransactionType.EXPENSE) "-" else "+") + "₹" + transaction.amountText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when (transaction.type) {
                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                TransactionType.INCOME -> Color(0xFF43A047)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
