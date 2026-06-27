package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.usecase.community.UpdateAuthorTrustScoreUseCase
import javax.inject.Inject

class ToggleVoteUseCase @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
    private val updateAuthorTrustScoreUseCase: UpdateAuthorTrustScoreUseCase
) {
    suspend operator fun invoke(postId: String, voteValue: Int, authorId: String): Result<Unit> {
        val userId = authRepository.getCurrentUser()?.id 
            ?: return Result.failure(Exception("Usuario no autenticado"))
            
        val result = feedRepository.toggleVote(userId, postId, voteValue)
        if (result.isSuccess) {
            updateAuthorTrustScoreUseCase(authorId)
        }
        return result
    }

}
