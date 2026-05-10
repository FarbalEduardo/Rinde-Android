package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFollowingUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(myUserId: String, targetUserId: String): Flow<Boolean> {
        return repository.isFollowing(myUserId, targetUserId)
    }
}
