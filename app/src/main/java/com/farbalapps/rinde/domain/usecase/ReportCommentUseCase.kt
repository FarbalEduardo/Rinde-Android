package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.model.ReportedComment
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.CommentRepository
import javax.inject.Inject

class ReportCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        postId: String,
        commentId: String,
        commentText: String,
        authorId: String
    ): Result<Unit> {
        val currentUser = authRepository.getCurrentUser() 
            ?: return Result.failure(Exception("Usuario no autenticado"))

        val reportedComment = ReportedComment(
            postId = postId,
            commentId = commentId,
            commentText = commentText,
            authorId = authorId,
            reporterId = currentUser.id,
            timestamp = System.currentTimeMillis()
        )

        return commentRepository.reportComment(reportedComment)
    }
}
