package com.unmsm.habitflow.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("habitflow_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            darkMode = prefs[DARK_MODE] ?: true,
            notifications = prefs[NOTIFICATIONS] ?: true,
            biometric = prefs[BIOMETRIC] ?: false,
            publicProfile = prefs[PUBLIC_PROFILE] ?: true,
            language = prefs[LANGUAGE] ?: "Espanol",
            accentColor = prefs[ACCENT_COLOR] ?: "violet"
        )
    }

    suspend fun setDarkMode(value: Boolean) = setBoolean(DARK_MODE, value)
    suspend fun setNotifications(value: Boolean) = setBoolean(NOTIFICATIONS, value)
    suspend fun setBiometric(value: Boolean) = setBoolean(BIOMETRIC, value)
    suspend fun setPublicProfile(value: Boolean) = setBoolean(PUBLIC_PROFILE, value)

    suspend fun setAccentColor(value: String) {
        context.dataStore.edit { prefs -> prefs[ACCENT_COLOR] = value }
    }

    private suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { prefs -> prefs[key] = value }
    }

    private companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val PUBLIC_PROFILE = booleanPreferencesKey("public_profile")
        val LANGUAGE = stringPreferencesKey("language")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
    }
}

data class SettingsState(
    val darkMode: Boolean = true,
    val notifications: Boolean = true,
    val biometric: Boolean = false,
    val publicProfile: Boolean = true,
    val language: String = "Espanol",
    val accentColor: String = "violet"
)
