package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import javax.inject.Inject

class ToggleSavePostUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String, postId: String, save: Boolean): Result<Unit> {
        return repository.toggleSavePost(userId, postId, save)
    }
}
