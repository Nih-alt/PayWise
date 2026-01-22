package com.nihal.paywise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.navigation.NotificationNavRequest
import com.nihal.paywise.ui.PayWiseApp
import com.nihal.paywise.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    
    private val navRequestFlow = MutableStateFlow<NotificationNavRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val onboardingViewModel: OnboardingViewModel = ViewModelProvider(this, AppViewModelProvider.Factory)[OnboardingViewModel::class.java]
        
        // Keep the splash screen on-screen until onboarding status is loaded
        splashScreen.setKeepOnScreenCondition {
            onboardingViewModel.isOnboardingCompleted.value == null
        }

        enableEdgeToEdge()
        
        handleIntent(intent)
        
        setContent {
            val navRequest by navRequestFlow.collectAsState()
            val onboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()
            
            if (onboardingCompleted != null) {
                PayWiseApp(
                    navRequest = navRequest,
                    onboardingCompleted = onboardingCompleted!!,
                    onNavRequestHandled = { navRequestFlow.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        
        val target = intent.getStringExtra("nav_target")
        val recurringId = intent.getStringExtra("recurringId")
        
        if (target != null) {
            Log.d("DeepLink", "Received request target: $target, recurringId: $recurringId")
            navRequestFlow.value = NotificationNavRequest(target, recurringId)
        }
    }
}