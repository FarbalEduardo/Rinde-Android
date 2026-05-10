package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import javax.inject.Inject

class UnfollowUserUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(myUserId: String, targetUserId: String): Result<Unit> {
        return repository.unfollowUser(myUserId, targetUserId)
    }
}
