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
enum class AppCurrency(val symbol: String, val code: String, val label: String) {
    CLP("$", "CLP", "Peso chileno (CLP)"),
    MXN("$", "MXN", "Peso mexicano (MXN)"),
    USD("$", "USD", "Dólar estadounidense (USD)"),
    EUR("€", "EUR", "Euro (EUR)")
}

class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")
        private val APP_CURRENCY = stringPreferencesKey("app_currency")
        private val BUNKER_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("bunker_mode")
        private val PRIVACY_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("privacy_mode")
    }

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(name)
    }

    val appLanguage: Flow<AppLanguage> = context.settingsDataStore.data.map { prefs ->
        val savedName = prefs[APP_LANGUAGE]
        if (savedName != null) {
            runCatching { AppLanguage.valueOf(savedName) }.getOrDefault(AppLanguage.ES)
        } else {
            // Detectar idioma del teléfono por defecto si el usuario nunca lo ha cambiado manualmente
            val systemLocale = java.util.Locale.getDefault().language.lowercase()
            if (systemLocale.startsWith("en")) AppLanguage.EN else AppLanguage.ES
        }
    }

    val appCurrency: Flow<AppCurrency> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[APP_CURRENCY] ?: AppCurrency.CLP.name
        runCatching { AppCurrency.valueOf(name) }.getOrDefault(AppCurrency.CLP)
    }

    val isBunkerMode: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[BUNKER_MODE] ?: false
    }

    val isPrivacyMode: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[PRIVACY_MODE] ?: false
    }

    fun getPrivacyMode(userId: String): Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        val key = if (userId.isNotEmpty()) androidx.datastore.preferences.core.booleanPreferencesKey("privacy_mode_$userId") else PRIVACY_MODE
        prefs[key] ?: false
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
        val localeTag = if (language == AppLanguage.EN) "en" else "es"
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(localeTag)
        )
    }

    suspend fun setAppCurrency(currency: AppCurrency) {
        context.settingsDataStore.edit { prefs ->
            prefs[APP_CURRENCY] = currency.name
        }
    }

    suspend fun setBunkerMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[BUNKER_MODE] = enabled
        }
    }

    suspend fun setPrivacyMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[PRIVACY_MODE] = enabled
        }
    }

    suspend fun setPrivacyMode(userId: String, enabled: Boolean) {
        val key = if (userId.isNotEmpty()) androidx.datastore.preferences.core.booleanPreferencesKey("privacy_mode_$userId") else PRIVACY_MODE
        context.settingsDataStore.edit { prefs ->
            prefs[key] = enabled
        }
    }
}
