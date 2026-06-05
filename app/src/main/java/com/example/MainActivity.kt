package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WaterReminderApp
import com.example.ui.WaterViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize notification channel support for Android 8+
        NotificationHelper.createNotificationChannel(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: WaterViewModel = viewModel()
                WaterReminderApp(viewModel)
            }
        }
    }
}

