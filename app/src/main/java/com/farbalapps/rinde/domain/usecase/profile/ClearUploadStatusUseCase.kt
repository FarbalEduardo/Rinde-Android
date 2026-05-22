package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import javax.inject.Inject

class ClearUploadStatusUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit> {
        return repository.clearUploadStatus(userId)
    }
}
