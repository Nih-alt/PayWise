package com.nihal.paywise.ui.addtxn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.BigAmountInput
import com.nihal.paywise.ui.components.SelectorRow
import com.nihal.paywise.ui.util.CategoryVisuals
import com.nihal.paywise.util.DateTimeFormatterUtil
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current
    
    // Bottom Sheet States
    var showAccountSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = viewModel.date.toEpochMilli()
    )

    // Derived States for UI
    val selectedAccount = remember(viewModel.selectedAccountId, accounts) {
        accounts.find { it.id == viewModel.selectedAccountId }
    }
    val selectedCategory = remember(viewModel.selectedCategoryId, categories) {
        categories.find { it.id == viewModel.selectedCategoryId }
    }
    
    val categoryVisual = remember(selectedCategory) {
        CategoryVisuals.getVisual(selectedCategory?.name ?: "")
    }
    
    val formattedDate = remember(viewModel.date) {
        DateTimeFormatterUtil.formatDate(viewModel.date)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp)
            ) {
                IconButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "New Expense",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { paddingValues ->
        AppBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Main Sheet Container
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Hero Amount Input
                            BigAmountInput(
                                value = viewModel.amountInput,
                                onValueChange = { viewModel.updateAmount(it) },
                                modifier = Modifier.padding(bottom = 32.dp)
                            )
                            
                            // Selectors
                            SelectorRow(
                                label = "Account",
                                selectedText = selectedAccount?.name ?: "Select Account",
                                icon = Icons.Default.AccountBalanceWallet,
                                onClick = { showAccountSheet = true },
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            SelectorRow(
                                label = "Category",
                                selectedText = selectedCategory?.name ?: "Select Category",
                                icon = categoryVisual.icon,
                                iconColor = categoryVisual.color,
                                iconContainerColor = categoryVisual.containerColor,
                                onClick = { showCategorySheet = true },
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            SelectorRow(
                                label = "Date",
                                selectedText = formattedDate,
                                icon = Icons.Default.CalendarToday,
                                onClick = { showDatePicker = true },
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            OutlinedTextField(
                                value = viewModel.note,
                                onValueChange = { viewModel.updateNote(it) },
                                label = { Text("Note (Optional)") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Button(
                                onClick = {
                                    viewModel.saveTransaction(onSuccess = navigateBack)
                                },
                                enabled = viewModel.canSave,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "Save Expense",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Account Selection Sheet
    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "Select Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { viewModel.seedDefaults() }) {
                            Text("No accounts. Tap to create defaults.")
                        }
                    }
                } else {
                    LazyColumn {
                        items(accounts) { account ->
                            ListItem(
                                headlineContent = { Text(account.name) },
                                leadingContent = {
                                    Icon(Icons.Default.AccountBalanceWallet, null)
                                },
                                trailingContent = {
                                    if (account.id == viewModel.selectedAccountId) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    viewModel.updateAccount(account.id)
                                    scope.launch { showAccountSheet = false }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Category Selection Sheet
    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                if (categories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { viewModel.seedDefaults() }) {
                            Text("No categories. Tap to create defaults.")
                        }
                    }
                } else {
                    LazyColumn {
                        items(categories) { category ->
                            val visual = CategoryVisuals.getVisual(category.name)
                            ListItem(
                                headlineContent = { Text(category.name) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(visual.containerColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(18.dp))
                                    }
                                },
                                trailingContent = {
                                    if (category.id == viewModel.selectedCategoryId) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    viewModel.updateCategory(category.id)
                                    scope.launch { showCategorySheet = false }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.updateDate(Instant.ofEpochMilli(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
