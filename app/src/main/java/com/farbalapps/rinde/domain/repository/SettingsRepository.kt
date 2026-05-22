package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.data.local.AppLanguage
import com.farbalapps.rinde.data.local.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    
    fun getAppLanguage(): Flow<AppLanguage>
    suspend fun setAppLanguage(language: AppLanguage)
    
    // Perfil Privado (Firestore + Local Sync)
    fun isProfilePrivate(): Flow<Boolean>
    suspend fun toggleProfilePrivacy(isPrivate: Boolean)
}
