package com.nihal.paywise.ui.addtxn

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Add Expense", modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = viewModel.amountInput,
            onValueChange = { viewModel.updateAmount(it) },
            label = { Text("Amount (Rupees)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Account Selector
        if (accounts.isEmpty()) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("No accounts found.")
                Button(onClick = { viewModel.seedDefaults() }) {
                    Text("Create default accounts")
                }
            }
        } else {
            var accountExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = !accountExpanded },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                val selectedAccount = accounts.find { it.id == viewModel.selectedAccountId }
                TextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.updateAccount(account.id)
                                accountExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Category Selector
        if (categories.isEmpty()) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text("No expense categories found.")
                Button(onClick = { viewModel.seedDefaults() }) {
                    Text("Create default categories")
                }
            }
        } else {
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                val selectedCategory = categories.find { it.id == viewModel.selectedCategoryId }
                TextField(
                    value = selectedCategory?.name ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.updateCategory(category.id)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = viewModel.note,
            onValueChange = { viewModel.updateNote(it) },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.saveTransaction(onSuccess = navigateBack)
            },
            enabled = viewModel.canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Expense")
        }
    }
}