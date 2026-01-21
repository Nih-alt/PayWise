package com.nihal.paywise

import android.app.Application
import com.nihal.paywise.data.local.DatabaseSeeder
import com.nihal.paywise.di.AppContainer
import com.nihal.paywise.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseTrackerApp : Application() {
    lateinit var container: AppContainer
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        
        // Seed default data
        applicationScope.launch {
            DatabaseSeeder(container.accountRepository, container.categoryRepository).seed()
        }
    }
}