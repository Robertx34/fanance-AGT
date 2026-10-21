package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainScreen
import com.example.ui.viewmodel.FinancialViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {

    private val financialViewModel: FinancialViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.scheduleDailyReminder(this)
        setContent {
            MainScreen(
                financialViewModel = financialViewModel,
                themeViewModel = themeViewModel
            )
        }
    }
}
