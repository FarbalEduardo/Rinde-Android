package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.CommentRepository
import javax.inject.Inject

class DeleteReplyUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(commentId: String, replyId: String, postId: String, authorId: String): Result<Unit> {
        val currentUser = authRepository.getCurrentUser() ?: return Result.failure(Exception("No autenticado"))
        if (currentUser.id != authorId) return Result.failure(Exception("Sin permisos"))
        return commentRepository.deleteReply(commentId, replyId, postId)
    }
}
