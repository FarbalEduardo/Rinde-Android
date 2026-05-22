package com.farbalapps.rinde.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AppLanguage { ES, EN }

class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(name)
    }

    val appLanguage: Flow<AppLanguage> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[APP_LANGUAGE] ?: AppLanguage.ES.name
        AppLanguage.valueOf(name)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { prefs ->
            prefs[APP_LANGUAGE] = language.name
        }
    }
}
