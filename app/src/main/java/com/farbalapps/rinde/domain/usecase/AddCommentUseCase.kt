package com.farbalapps.rinde.domain.usecase

import android.net.Uri
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
        imageUri: Uri? = null
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
            localImageUri = imageUri
        ).map { comment } // Devolver el comentario (aunque el ID sea temporal, el repositorio lo actualizará o se recargará del Flow)
    }

}
