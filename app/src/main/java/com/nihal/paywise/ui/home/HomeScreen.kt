package com.nihal.paywise.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.di.AppViewModelProvider

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onRecurringClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val transactions by viewModel.transactions.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Home", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onRecurringClick) {
                    Text("Recurring")
                }
            }
            
            Spacer(modifier = Modifier.padding(8.dp))

            if (transactions.isEmpty()) {
                Text("No transactions this month.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(transactions) { transaction ->
                        TransactionItem(transaction)
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: HomeTransactionUiModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        // Line 1: Amount (Bold)
        Text(
            text = transaction.amountText,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        // Line 2: Category
        Text(
            text = transaction.categoryName,
            fontSize = 14.sp
        )
        // Line 3: Account
        Text(
            text = transaction.accountName,
            color = Color.Gray,
            fontSize = 12.sp
        )
        // Line 4: Date + Time
        Text(
            text = "${transaction.dateText} • ${transaction.timeText}",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}