package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.CommentRepository
import javax.inject.Inject

class ToggleCommentLikeUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    suspend fun toggleCommentLike(postId: String, commentId: String): Result<Unit> {
        val userId = authRepository.getCurrentUser()?.id
            ?: return Result.failure(Exception("Usuario no autenticado"))
            
        return commentRepository.toggleCommentLike(userId, commentId, postId)
    }

    suspend fun toggleReplyLike(commentId: String, replyId: String): Result<Unit> {
        val userId = authRepository.getCurrentUser()?.id
            ?: return Result.failure(Exception("Usuario no autenticado"))
            
        return commentRepository.toggleReplyLike(userId, replyId, commentId)
    }
}
