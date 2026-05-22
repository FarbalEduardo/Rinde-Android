package com.farbalapps.rinde.domain.usecase.settings

import com.farbalapps.rinde.data.local.ThemeMode
import com.farbalapps.rinde.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repository.getThemeMode()
}

class SetThemeUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class GetLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<com.farbalapps.rinde.data.local.AppLanguage> = repository.getAppLanguage()
}

class SetLanguageUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(language: com.farbalapps.rinde.data.local.AppLanguage) = repository.setAppLanguage(language)
}

class IsProfilePrivateUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.isProfilePrivate()
}

class TogglePrivacyUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(isPrivate: Boolean) = repository.toggleProfilePrivacy(isPrivate)
}
