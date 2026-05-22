package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.model.Profile
import com.farbalapps.rinde.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBlockedUsersUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(userId: String): Flow<List<Profile>> = repository.getBlockedUsers(userId)
}
