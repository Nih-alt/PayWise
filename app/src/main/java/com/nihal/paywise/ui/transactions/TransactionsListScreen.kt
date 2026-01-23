package com.nihal.paywise.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.ui.util.CategoryVisuals
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    onTransactionClick: (String) -> Unit,
    onAddClick: (String) -> Unit,
    viewModel: TransactionsListViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    val filteredAccount = accounts.find { it.id == uiState.selectedAccountId }
    var cardStatement by remember { mutableStateOf<CardStatement?>(null) }
    
    // Fetch statement if a card is selected
    val getCardStatementUseCase = (LocalContext.current.applicationContext as com.nihal.paywise.ExpenseTrackerApp).container.getCardStatementUseCase
    LaunchedEffect(uiState.selectedAccountId) {
        if (filteredAccount?.type == AccountType.CARD) {
            getCardStatementUseCase(filteredAccount, java.time.YearMonth.now()).collect {
                cardStatement = it
            }
        } else {
            cardStatement = null
        }
    }
    
    var showFilters by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }

    if (showAddSheet) {
        AddActionSheet(
            onDismiss = { showAddSheet = false },
            onActionSelected = { type ->
                showAddSheet = false
                onAddClick(type)
            }
        )
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))) {
                        CenterAlignedTopAppBar(
                            title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                            actions = {
                                IconButton(onClick = { showFilters = !showFilters }) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                        )
                        
                        SearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::setSearchQuery,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        if (showFilters) {
                            FilterRow(
                                uiState = uiState,
                                accounts = accounts,
                                categories = categories,
                                onTypeSelect = viewModel::setTypeFilter,
                                onAccountSelect = viewModel::setAccountFilter,
                                onCategorySelect = viewModel::setCategoryFilter
                            )
                        }
                    }
                }
            ) { padding ->
                if (uiState.groupedTransactions.isEmpty() && !uiState.isLoading) {
                    EmptyTransactions { showAddSheet = true }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        cardStatement?.let {
                            item {
                                com.nihal.paywise.ui.accounts.CardSummaryGlass(it, onPayClick = { onAddClick("TRANSFER") })
                            }
                        }

                        uiState.groupedTransactions.forEach { (date, items) ->
                            item {
                                DateHeader(date)
                            }
                            items(items) { item ->
                                TransactionItem(item, onClick = { onTransactionClick(item.transaction.id) })
                            }
                        }
                    }
                }
            }

            // Unified Anchored Add Button (same as Home for consistency)
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
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search note, category, account...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null) } },
        shape = CircleShape,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterRow(
    uiState: TransactionsListUiState,
    accounts: List<Account>,
    categories: List<Category>,
    onTypeSelect: (TransactionType?) -> Unit,
    onAccountSelect: (String?) -> Unit,
    onCategorySelect: (String?) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Type Filters
        FilterChip(
            selected = uiState.typeFilter == null,
            onClick = { onTypeSelect(null) },
            label = { Text("All") }
        )
        TransactionType.entries.forEach { type ->
            FilterChip(
                selected = uiState.typeFilter == type,
                onClick = { onTypeSelect(type) },
                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
fun DateHeader(date: LocalDate) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM")
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(formatter)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun TransactionItem(item: TransactionListItem, onClick: () -> Unit) {
    val visual = CategoryVisuals.getVisual(item.categoryName)
    GlassCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(visual.containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.transaction.note ?: item.categoryName, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = (if (item.transaction.type == TransactionType.EXPENSE) "-" else if (item.transaction.type == TransactionType.INCOME) "+" else "") + item.amountText,
                fontWeight = FontWeight.Black,
                color = when (item.transaction.type) {
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    TransactionType.INCOME -> Color(0xFF43A047)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun EmptyTransactions(onAddClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Default.ReceiptLong,
            title = "No transactions yet",
            subtitle = "Your spending history will appear here.",
            hint = "Tap the Add button below to start tracking"
        )
    }
}
