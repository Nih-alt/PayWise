package com.nihal.paywise.ui.recurring

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddRecurringViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Add Recurring",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = viewModel.title,
            onValueChange = { viewModel.updateTitle(it) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = viewModel.amountInput,
            onValueChange = { viewModel.updateAmount(it) },
            label = { Text("Amount (Rupees)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Due Day Selector
        var dueDayExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = dueDayExpanded,
            onExpandedChange = { dueDayExpanded = !dueDayExpanded },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            val displayValue = if (viewModel.dueDay == -1) "Last Day of Month" else "Day ${viewModel.dueDay}"
            TextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                label = { Text("Due Day") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dueDayExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = dueDayExpanded,
                onDismissRequest = { dueDayExpanded = false }
            ) {
                (1..31).forEach { day ->
                    DropdownMenuItem(
                        text = { Text("Day $day") },
                        onClick = {
                            viewModel.updateDueDay(day)
                            dueDayExpanded = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Last Day of Month") },
                    onClick = {
                        viewModel.updateDueDay(-1)
                        dueDayExpanded = false
                    }
                )
            }
        }

        // Account Selector
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
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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

        // Category Selector
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
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
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

        OutlinedTextField(
            value = viewModel.leadDays,
            onValueChange = { viewModel.updateLeadDays(it) },
            label = { Text("Lead Days (for reminders)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Auto-post on due date", modifier = Modifier.weight(1f))
            Switch(
                checked = viewModel.autoPost,
                onCheckedChange = { viewModel.updateAutoPost(it) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.saveRecurring(onSuccess = navigateBack) },
            enabled = viewModel.canSave,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Save Recurring")
        }
    }
}
