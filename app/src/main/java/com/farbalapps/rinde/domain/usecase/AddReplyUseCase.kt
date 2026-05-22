package com.farbalapps.rinde.domain.usecase

import android.net.Uri
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.CommentRepository
import javax.inject.Inject

class AddReplyUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        commentId: String,
        postId: String,
        text: String,
        mentionedUser: String? = null,
        imageUri: Uri? = null
    ): Result<Reply> {
        val user = authRepository.getCurrentUser()
            ?: return Result.failure(Exception("Usuario no autenticado"))

        val reply = Reply(
            id = "",
            authorId = user.id,
            authorName = user.displayName ?: "Usuario",
            authorPhotoUrl = user.photoUrl,
            text = text,
            timestamp = System.currentTimeMillis(),
            mentionedUser = mentionedUser
        )

        return commentRepository.addReply(
            commentId = commentId,
            reply = reply,
            localImageUri = imageUri
        ).map { reply }
    }

}
