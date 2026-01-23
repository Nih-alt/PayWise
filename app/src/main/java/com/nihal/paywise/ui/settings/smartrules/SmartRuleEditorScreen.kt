package com.nihal.paywise.ui.settings.smartrules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.data.local.entity.SmartMatchMode
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRuleEditorScreen(
    onBack: () -> Unit,
    viewModel: SmartRuleEditorViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (viewModel.isEditMode) "Edit Rule" else "New Rule", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = viewModel.matchText,
                                onValueChange = { viewModel.matchText = it },
                                label = { Text("Match Keyword") },
                                placeholder = { Text("e.g. Swiggy") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Match Mode", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmartMatchMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = viewModel.matchMode == mode,
                                        onClick = { viewModel.matchMode = mode },
                                        label = { Text(mode.name) }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionHeader(title = "Auto-Fill Outputs")
                    GlassCard {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            SelectorRow(
                                label = "Category",
                                selectedText = categories.find { it.id == viewModel.selectedCategoryId }?.name ?: "No Change",
                                icon = Icons.Default.Category,
                                onClick = { /* Show Category Picker */ }
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            SelectorRow(
                                label = "Account",
                                selectedText = accounts.find { it.id == viewModel.selectedAccountId }?.name ?: "No Change",
                                icon = Icons.Default.AccountBalanceWallet,
                                onClick = { /* Show Account Picker */ }
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.saveRule(onBack) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.large,
                        enabled = viewModel.matchText.isNotBlank()
                    ) {
                        Text("Save Rule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
