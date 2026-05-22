package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProfilePostsUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(userId: String): Flow<List<CommunityPost>> {
        return repository.getProfilePosts(userId)
    }
}
