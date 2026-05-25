package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ContactViewModel
import com.example.ui.DebtSyncNavigation
import com.example.ui.theme.LocalIsDarkMode
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ContactViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            CompositionLocalProvider(LocalIsDarkMode provides isDarkMode) {
                MyApplicationTheme {
                    DebtSyncNavigation(viewModel)
                }
            }
        }
    }
}
