package com.farbalapps.rinde.domain.usecase

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
        imageUri: String? = null   // URI como String — mapeo a android.net.Uri ocurre en la capa Data
    ): Result<Reply> {
        val user = authRepository.getCurrentUser()
            ?: return Result.failure(Exception("Usuario no autenticado"))

        val reply = Reply(
            id = "",
            commentId = commentId,
            postId = postId,
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
            imageUri = imageUri
        ).map { reply }
    }

}
