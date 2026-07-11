package com.unmsm.habitflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import com.unmsm.habitflow.ui.navigation.HabitFlowApp
import com.unmsm.habitflow.ui.theme.HabitFlowTheme
import com.unmsm.habitflow.ui.viewmodel.ThemeViewModel
import kotlin.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by themeViewModel.state.collectAsState()
            HabitFlowTheme(
                darkTheme = themeState.darkMode,
                accentColor = themeState.accentColor
            ) {
                HabitFlowApp()
            }
        }
    }
}
