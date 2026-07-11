package com.unmsm.habitflow.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unmsm.habitflow.ui.theme.HabitFlowAccent
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
            themeMode = prefs[THEME_MODE]
                ?: prefs[DARK_MODE]?.let { if (it) "dark" else "light" }
                ?: "system",
            notifications = prefs[NOTIFICATIONS] ?: true,
            biometric = prefs[BIOMETRIC] ?: false,
            publicProfile = prefs[PUBLIC_PROFILE] ?: true,
            language = prefs[LANGUAGE] ?: "Español (Perú)",
            accentColor = prefs[ACCENT_COLOR] ?: HabitFlowAccent.Mint.key,
            dynamicColor = prefs[DYNAMIC_COLOR] ?: false,
            textScale = prefs[TEXT_SCALE] ?: "standard",
            voiceResponseEnabled = prefs[VOICE_RESPONSE] ?: true,
            onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun setDarkMode(value: Boolean) = setThemeMode(if (value) "dark" else "light")
    suspend fun setNotifications(value: Boolean) = setBoolean(NOTIFICATIONS, value)
    suspend fun setBiometric(value: Boolean) = setBoolean(BIOMETRIC, value)
    suspend fun setPublicProfile(value: Boolean) = setBoolean(PUBLIC_PROFILE, value)
    suspend fun setVoiceResponseEnabled(value: Boolean) = setBoolean(VOICE_RESPONSE, value)
    suspend fun setOnboardingCompleted(value: Boolean) = setBoolean(ONBOARDING_COMPLETED, value)

    suspend fun setAccentColor(value: String) {
        context.dataStore.edit { prefs -> prefs[ACCENT_COLOR] = value }
    }

    suspend fun setThemeMode(value: String) {
        if (value !in THEME_MODES) return
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = value }
    }

    suspend fun setDynamicColor(value: Boolean) = setBoolean(DYNAMIC_COLOR, value)

    suspend fun setTextScale(value: String) {
        if (value !in TEXT_SCALES) return
        context.dataStore.edit { prefs -> prefs[TEXT_SCALE] = value }
    }

    private suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { prefs -> prefs[key] = value }
    }

    private companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val PUBLIC_PROFILE = booleanPreferencesKey("public_profile")
        val VOICE_RESPONSE = booleanPreferencesKey("voice_response")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LANGUAGE = stringPreferencesKey("language")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val THEME_MODES = setOf("system", "light", "dark")
        val TEXT_SCALES = setOf("compact", "standard", "large")
    }
}

data class SettingsState(
    val themeMode: String = "system",
    val notifications: Boolean = true,
    val biometric: Boolean = false,
    val publicProfile: Boolean = true,
    val language: String = "Español (Perú)",
    val accentColor: String = HabitFlowAccent.Mint.key,
    val dynamicColor: Boolean = false,
    val textScale: String = "standard",
    val voiceResponseEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
) {
    val darkMode: Boolean get() = themeMode == "dark"
}
