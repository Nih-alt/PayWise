package com.nihal.paywise.ui.addtxn

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.R
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.ui.components.*
import com.nihal.paywise.ui.util.CategoryVisuals
import com.nihal.paywise.util.DateTimeFormatterUtil
import kotlinx.coroutines.launch
import java.time.Instant

data class PickerItem(val id: String, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorScreen(
    navigateBack: () -> Unit,
    viewModel: TransactionEditorViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val context = LocalContext.current
    
    var showAccountSheet by mutableStateOf(false)
    var showCounterAccountSheet by mutableStateOf(false)
    var showCategorySheet by mutableStateOf(false)
    var showDatePicker by mutableStateOf(false)
    var showDeleteConfirm by mutableStateOf(false)

    val selectedAccount = accounts.find { it.id == uiState.selectedAccountId }
    val selectedCounterAccount = accounts.find { it.id == uiState.selectedCounterAccountId }
    val selectedCategory = categories.find { it.id == uiState.selectedCategoryId }
    val catVisual = CategoryVisuals.getVisual(selectedCategory?.name ?: "")

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTransaction(navigateBack) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (uiState.isEditMode) "Edit Transaction" 
                                   else "New ${uiState.transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (uiState.isEditMode) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            BigAmountInput(
                                value = uiState.amountInput,
                                onValueChange = { viewModel.updateAmount(it) }
                            )
                        }
                    }
                }

                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            SelectorRow(
                                label = if (uiState.transactionType == TransactionType.TRANSFER) "From Account" else "Account",
                                selectedText = selectedAccount?.name ?: "Select Account",
                                icon = Icons.Default.AccountBalanceWallet,
                                onClick = { showAccountSheet = true }
                            )
                            
                            if (uiState.transactionType == TransactionType.TRANSFER) {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                SelectorRow(
                                    label = "To Account",
                                    selectedText = selectedCounterAccount?.name ?: "Select Account",
                                    icon = Icons.Default.SwapHoriz,
                                    onClick = { showCounterAccountSheet = true }
                                )
                            }

                            if (uiState.transactionType != TransactionType.TRANSFER) {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                SelectorRow(
                                    label = "Category",
                                    selectedText = selectedCategory?.name ?: "Select Category",
                                    icon = catVisual.icon,
                                    iconColor = catVisual.color,
                                    iconContainerColor = catVisual.containerColor,
                                    onClick = { showCategorySheet = true }
                                )
                            }

                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            SelectorRow(
                                label = "Date",
                                selectedText = DateTimeFormatterUtil.formatDate(uiState.date),
                                icon = Icons.Default.CalendarToday,
                                onClick = { showDatePicker = true }
                            )
                        }
                    }
                }

                item {
                    GlassCard {
                        OutlinedTextField(
                            value = uiState.note,
                            onValueChange = { viewModel.updateNote(it) },
                            placeholder = { Text("Add a note...") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                item {
                    AttachmentSection(
                        attachments = uiState.attachments,
                        filesDir = context.filesDir,
                        onAddClick = { launcher.launch(arrayOf("image/*", "application/pdf")) },
                        onRemoveClick = { viewModel.removeAttachment(it) },
                        onAttachmentClick = { /* Handle preview */ }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.saveTransaction(navigateBack) },
                        enabled = viewModel.validate(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Save Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAccountSheet) {
        ModalBottomSheet(onDismissRequest = { showAccountSheet = false }) {
            PickerList(
                title = "Select Account",
                items = accounts.map { PickerItem(it.id, it.name) },
                selectedId = uiState.selectedAccountId,
                onSelect = { viewModel.updateAccount(it); showAccountSheet = false }
            )
        }
    }

    if (showCounterAccountSheet) {
        ModalBottomSheet(onDismissRequest = { showCounterAccountSheet = false }) {
            PickerList(
                title = "Select Destination Account",
                items = accounts.map { PickerItem(it.id, it.name) },
                selectedId = uiState.selectedCounterAccountId,
                onSelect = { viewModel.updateCounterAccount(it); showCounterAccountSheet = false }
            )
        }
    }

    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }) {
            PickerList(
                title = "Select Category",
                items = categories.map { PickerItem(it.id, it.name) },
                selectedId = uiState.selectedCategoryId,
                onSelect = { viewModel.updateCategory(it); showCategorySheet = false }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.date.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDate(Instant.ofEpochMilli(it)) }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun PickerList(
    title: String,
    items: List<PickerItem>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        LazyColumn {
            items(items) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    trailingContent = { if (item.id == selectedId) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSelect(item.id) }
                )
            }
        }
    }
}