package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import javax.inject.Inject

class UpdatePrivacyUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String, isPrivate: Boolean): Result<Unit> {
        return repository.updatePrivacy(userId, isPrivate)
    }
}
