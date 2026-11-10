package com.medusko.rivla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.medusko.rivla.ui.MainScreen
import com.medusko.rivla.ui.RivlaViewModel
import com.medusko.rivla.ui.theme.RivlaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RivlaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            RivlaTheme(darkTheme = isDarkMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
