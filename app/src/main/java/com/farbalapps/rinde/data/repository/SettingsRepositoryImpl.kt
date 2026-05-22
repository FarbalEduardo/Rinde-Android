package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.local.AppLanguage
import com.farbalapps.rinde.data.local.SessionManager
import com.farbalapps.rinde.data.local.SettingsManager
import com.farbalapps.rinde.data.local.ThemeMode
import com.farbalapps.rinde.domain.repository.ProfileRepository
import com.farbalapps.rinde.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsManager: SettingsManager,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : SettingsRepository {

    override fun getThemeMode(): Flow<ThemeMode> = settingsManager.themeMode
    override suspend fun setThemeMode(mode: ThemeMode) = settingsManager.setThemeMode(mode)

    override fun getAppLanguage(): Flow<AppLanguage> = settingsManager.appLanguage
    override suspend fun setAppLanguage(language: AppLanguage) = settingsManager.setAppLanguage(language)

    override fun isProfilePrivate(): Flow<Boolean> {
        return sessionManager.userId.flatMapLatest { userId ->
            if (userId.isNotEmpty()) {
                profileRepository.getProfile(userId).map { it.isPrivate }
            } else {
                flowOf(false)
            }
        }
    }

    override suspend fun toggleProfilePrivacy(isPrivate: Boolean) {
        val userId = sessionManager.userId.first()
        if (userId.isNotEmpty()) {
            profileRepository.updatePrivacy(userId, isPrivate)
        }
    }
}
