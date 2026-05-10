package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import javax.inject.Inject

class FollowUserUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(myUserId: String, targetUserId: String): Result<Unit> {
        return repository.followUser(myUserId, targetUserId)
    }
}
