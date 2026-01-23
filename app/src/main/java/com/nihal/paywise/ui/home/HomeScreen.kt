package com.nihal.paywise.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.R
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.ui.goals.GoalsViewModel
import com.nihal.paywise.ui.util.CategoryVisuals
import com.nihal.paywise.util.MoneyFormatter

@Composable
fun HomeScreen(
    onAddClick: (String) -> Unit,
    onAddSalaryClick: (String) -> Unit,
    onRecurringClick: () -> Unit,
    onBudgetClick: () -> Unit,
    onGoalsClick: () -> Unit,
    onClaimsClick: () -> Unit,
    onFabVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val categories by viewModel.categorySummary.collectAsState()
    val plannedVsActual by viewModel.plannedVsActual.collectAsState()
    val showSalaryPrompt by viewModel.showSalaryPrompt.collectAsState()
    val cardAlerts by viewModel.cardDueAlerts.collectAsState()
    val cardBillAlerts by viewModel.cardBillAlerts.collectAsState()
    val pendingClaimsTotal by viewModel.pendingClaimsTotalPaise.collectAsState()
    
    val goalsViewModel: GoalsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val goals by goalsViewModel.activeGoals.collectAsState()
    
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(transactions) {
        onFabVisibilityChange(false)
    }

    if (showAddSheet) {
        AddActionSheet(
            onDismiss = { showAddSheet = false },
            onActionSelected = { type ->
                showAddSheet = false
                onAddClick(type)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Hero Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeroCard(
                    month = summary.cycleLabel,
                    totalSpent = summary.totalExpense,
                    summary = summary,
                    onBudgetClick = onBudgetClick
                )
            }

            // Salary Prompt
            item {
                AnimatedVisibility(
                    visible = showSalaryPrompt,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.missing_salary_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.missing_salary_subtitle), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onAddSalaryClick(summary.cycleLabel) }) {
                                Text(stringResource(R.string.add_now))
                            }
                        }
                    }
                }
            }

            // 2. Quick Actions
            item {
                SectionHeader(title = stringResource(R.string.quick_actions))
                QuickActionsRow(
                    onAddExpense = { showAddSheet = true },
                    onAddSalary = { onAddSalaryClick(summary.cycleLabel) },
                    onRecurring = onRecurringClick
                )
            }

            // Planned vs Actual
            if (plannedVsActual != null) {
                item {
                    SectionHeader(title = stringResource(R.string.planned_commitments))
                    PlannedVsActualCard(plannedVsActual!!)
                }
            }

            // Credit Card Dues
            if (cardAlerts.isNotEmpty()) {
                item {
                    SectionHeader(title = "Credit Card Dues")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cardAlerts.forEach { alert ->
                            CardDueAlertItem(alert, onCardClick = { onAddClick("CARD_DETAILS_${alert.accountId}") })
                        }
                    }
                }
            }

            if (cardBillAlerts.isNotEmpty()) {
                item {
                    SectionHeader(title = "Outstanding Bills")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cardBillAlerts.forEach { (account, bill) ->
                            CardBillAlertItem(account, bill, onCardClick = { onAddClick("CARD_BILL_${account.id}") })
                        }
                    }
                }
            }

            // Reimbursements
            if (pendingClaimsTotal > 0) {
                item {
                    SectionHeader(title = "Reimbursements", actionText = "View All", onActionClick = onClaimsClick)
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onClaimsClick() },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assignment, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pending Claims", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Total expected: ${MoneyFormatter.formatPaise(pendingClaimsTotal)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Savings Progress
            if (goals.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Savings Goals",
                        actionText = "View All",
                        onActionClick = onGoalsClick
                    )
                    com.nihal.paywise.ui.goals.GoalCard(goals.first(), onClick = onGoalsClick)
                }
            }

            // 3. Category Summary
            if (categories.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.top_spending))
                    CategorySummaryRow(categories)
                }
            }

            // 4. Recent Transactions
            item {
                SectionHeader(
                    title = stringResource(R.string.recent_activity),
                    actionText = if (transactions.isEmpty()) null else stringResource(R.string.see_all),
                    onActionClick = { /* Navigate to all transactions */ }
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmptyState(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.no_transactions),
                            subtitle = stringResource(R.string.no_transactions_subtitle),
                            hint = "Tap the Add button below to start tracking"
                        )
                    }
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

        // Unified Anchored Add Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp,
            onClick = { showAddSheet = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HeroCard(
    month: String,
    totalSpent: String,
    summary: HomeSummaryUiModel,
    onBudgetClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
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
                if (summary.isSalaryCycle) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            stringResource(R.string.salary_based),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                
                val statusText = when(summary.budgetStatus) {
                    HomeBudgetStatus.NOT_SET -> stringResource(R.string.budget_not_set)
                    HomeBudgetStatus.ON_TRACK -> stringResource(R.string.budget_on_track)
                    HomeBudgetStatus.NEAR_LIMIT -> stringResource(R.string.budget_near_limit)
                    HomeBudgetStatus.OVER_BUDGET -> stringResource(R.string.budget_over_budget)
                }
                
                val statusColor = when(summary.budgetStatus) {
                    HomeBudgetStatus.NOT_SET -> MaterialTheme.colorScheme.secondary
                    HomeBudgetStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
                    HomeBudgetStatus.NEAR_LIMIT -> MaterialTheme.colorScheme.tertiary
                    HomeBudgetStatus.OVER_BUDGET -> MaterialTheme.colorScheme.error
                }

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = CircleShape,
                    onClick = onBudgetClick
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.total_spent),
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
                        stringResource(R.string.income),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(summary.totalIncome, style = MaterialTheme.typography.titleSmall)
                }
                Column {
                    Text(
                        stringResource(R.string.savings),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(summary.savings, style = MaterialTheme.typography.titleSmall)
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { onBudgetClick() }
                ) {
                    Text(
                        stringResource(R.string.budget),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (summary.budgetPercent != null) "${summary.budgetPercent}%" else stringResource(R.string.set),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (summary.budgetStatus == HomeBudgetStatus.OVER_BUDGET) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun PlannedVsActualCard(model: HomePlannedVsActualUiModel) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.planned_commitments), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text("${stringResource(R.string.planned)}: ${model.plannedAmountText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("${stringResource(R.string.posted)}: ${model.actualAmountText}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (model.isOverPlanned) "+ ${model.deltaText}" else "- ${model.deltaText}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (model.isOverPlanned) MaterialTheme.colorScheme.error else Color(0xFF43A047)
                )
                Text(
                    text = if (model.isOverPlanned) stringResource(R.string.above_plan) else stringResource(R.string.under_plan),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { model.progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = if (model.progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionsRow(onAddExpense: () -> Unit, onAddSalary: () -> Unit, onRecurring: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val maxItems = when {
        screenWidthDp < 360 -> 1
        screenWidthDp < 600 -> 2
        else -> 4
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = maxItems
    ) {
        val itemModifier = Modifier.weight(1f)

        ActionChip(
            icon = Icons.Default.Add,
            label = stringResource(R.string.action_expense),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            onClick = onAddExpense,
            modifier = itemModifier
        )
        ActionChip(
            icon = Icons.Default.AccountBalance,
            label = stringResource(R.string.action_salary),
            containerColor = Color(0xFFE8F5E9),
            contentColor = Color(0xFF2E7D32),
            onClick = onAddSalary,
            modifier = itemModifier
        )
        ActionChip(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            label = stringResource(R.string.action_transfer),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            enabled = false,
            onClick = {},
            modifier = itemModifier
        )
        ActionChip(
            icon = Icons.Default.Refresh,
            label = stringResource(R.string.action_recurring),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onRecurring,
            modifier = itemModifier
        )
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
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f),
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
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
                Text(category.totalAmount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
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
fun CardDueAlertItem(statement: CardStatement, onCardClick: () -> Unit) {
    val color = if (statement.status == CardPaymentStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CreditCard, null, tint = color)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(statement.accountName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Due: ${MoneyFormatter.formatPaise(statement.netDuePaise)} by ${statement.dueDate}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (statement.status == CardPaymentStatus.OVERDUE) "OVERDUE" else "DUE SOON",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun CardBillAlertItem(
    account: Account,
    bill: com.nihal.paywise.domain.usecase.CardBillUiModel,
    onCardClick: () -> Unit
) {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ReceiptLong, null, tint = color)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Upcoming Bill: ${MoneyFormatter.formatPaise(bill.remainingToPayPaise)}", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
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
            text = (if (transaction.type == TransactionType.EXPENSE) "-" else "+") + transaction.amountText,
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
