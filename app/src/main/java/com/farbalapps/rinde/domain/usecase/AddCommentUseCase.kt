package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.CommentRepository
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        postId: String,
        text: String,
        imageUri: String? = null   // URI como String — mapeo a android.net.Uri ocurre en la capa Data
    ): Result<Comment> {
        val user = authRepository.getCurrentUser() 
            ?: return Result.failure(Exception("Usuario no autenticado"))
            
        val comment = Comment(
            id = "", // Generado por el repositorio
            authorId = user.id,
            authorName = user.displayName ?: "Usuario",
            authorPhotoUrl = user.photoUrl,
            text = text,
            timestamp = System.currentTimeMillis()
        )
            
        return commentRepository.addComment(
            postId = postId,
            comment = comment,
            imageUri = imageUri
        ).map { comment }
    }

}
