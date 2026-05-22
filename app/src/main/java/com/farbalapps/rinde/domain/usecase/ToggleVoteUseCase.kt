package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

class ToggleVoteUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(postId: String, voteValue: Int): Result<Unit> {
        val userId = authRepository.getCurrentUser()?.id 
            ?: return Result.failure(Exception("Usuario no autenticado"))
            
        return feedRepository.toggleVote(userId, postId, voteValue)
    }

}
