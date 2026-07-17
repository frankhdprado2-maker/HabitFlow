package com.unmsm.habitflow

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.isSystemInDarkTheme
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
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
        enableEdgeToEdge()
        setContent {
            val themeState by themeViewModel.state.collectAsState()
            val systemDarkTheme = isSystemInDarkTheme()
            HabitFlowTheme(
                darkTheme = when (themeState.themeMode) {
                    "dark" -> true
                    "light" -> false
                    else -> systemDarkTheme
                },
                accentColor = themeState.accentColor,
                dynamicColor = themeState.dynamicColor,
                textScale = themeState.textScale
            ) {
                HabitFlowApp()
            }
        }
    }

    companion object { private const val NOTIFICATION_PERMISSION_REQUEST = 2001 }
}
