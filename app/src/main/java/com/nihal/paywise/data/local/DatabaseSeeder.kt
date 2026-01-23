package com.nihal.paywise.data.local

import android.util.Log
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.AccountType
import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.CategoryKind
import com.nihal.paywise.domain.model.SpendingGroup
import java.util.UUID

class DatabaseSeeder(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) {
    private val TAG = "DatabaseSeeder"

    suspend fun seed() {
        seedAccounts()
        seedCategories()
    }

    private suspend fun seedAccounts() {
        if (accountRepository.getAccountCount() == 0) {
            Log.d(TAG, "Seeding default accounts...")
            val defaults = listOf(
                Account(UUID.randomUUID().toString(), "Cash", AccountType.CASH, 0),
                Account(UUID.randomUUID().toString(), "Bank / UPI", AccountType.BANK, 0),
                Account(UUID.randomUUID().toString(), "Credit Card", AccountType.CARD, 0)
            )
            defaults.forEach { accountRepository.insertAccount(it) }
            Log.d(TAG, "Default accounts seeded.")
        }
    }

    private suspend fun seedCategories() {
        // Seed Expense Categories
        if (categoryRepository.getCategoryCountByKind(CategoryKind.EXPENSE) == 0) {
            Log.d(TAG, "Seeding default expense categories...")
            val fixed = listOf("Rent", "EMI", "Utilities", "Mobile", "Internet", "DTH", "Insurance", "Subscriptions", "Kids/School", "Fees")
            val discretionary = listOf("Groceries", "Fuel", "Commute", "Maintenance/Society", "Dining", "Shopping", "Medical", "Gifts", "Travel", "Household Help", "Parking/Toll", "Taxes", "Misc")
            
            fixed.forEach { name ->
                categoryRepository.insertCategory(
                    Category(UUID.randomUUID().toString(), name, 0xFF808080, CategoryKind.EXPENSE, SpendingGroup.FIXED, null)
                )
            }
            discretionary.forEach { name ->
                categoryRepository.insertCategory(
                    Category(UUID.randomUUID().toString(), name, 0xFF808080, CategoryKind.EXPENSE, SpendingGroup.DISCRETIONARY, null)
                )
            }
            Log.d(TAG, "Expense categories seeded.")
        }

        // Seed Income Categories
        if (categoryRepository.getCategoryCountByKind(CategoryKind.INCOME) == 0) {
            Log.d(TAG, "Seeding default income categories...")
            val incomes = listOf("Salary", "Bonus", "Interest", "Refunds/Reimbursements")
            incomes.forEach { name ->
                categoryRepository.insertCategory(
                    Category(UUID.randomUUID().toString(), name, 0xFF4CAF50, CategoryKind.INCOME, SpendingGroup.DISCRETIONARY, null)
                )
            }
            Log.d(TAG, "Income categories seeded.")
        }
    }
}
