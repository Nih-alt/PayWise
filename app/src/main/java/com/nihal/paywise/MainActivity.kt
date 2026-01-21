package com.nihal.paywise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nihal.paywise.ui.PayWiseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val navTarget = intent.getStringExtra("nav_target")
        setContent {
            PayWiseApp(navTarget = navTarget)
        }
    }
}