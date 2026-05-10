package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import javax.inject.Inject

class BlockUserUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(userId: String, targetUserId: String): Result<Unit> {
        return repository.blockUser(userId, targetUserId)
    }
}
