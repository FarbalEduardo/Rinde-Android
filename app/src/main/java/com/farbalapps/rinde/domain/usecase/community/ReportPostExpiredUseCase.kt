package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * Caso de uso responsable de notificar al autor que otros miembros de la comunidad han reportado su oferta como expirada.
 *
 * @property feedRepository Repositorio de ofertas y feed comunitario.
 */
class ReportPostExpiredUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    /**
     * Envía una notificación de reporte de expiración al autor de la publicación.
     *
     * @param postId Identificador del post.
     * @param postTitle Título de la oferta reportada.
     * @param authorId Identificador del autor de la publicación.
     * @param currentUserId Identificador del usuario que emite el reporte.
     * @param currentUserName Nombre del usuario que emite el reporte.
     * @return [Result] con el resultado de la emisión de la notificación.
     */
    suspend operator fun invoke(
        postId: String,
        postTitle: String,
        authorId: String,
        currentUserId: String,
        currentUserName: String
    ): Result<Unit> {
        if (postId.isBlank() || authorId.isBlank() || currentUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Identifiers must not be blank"))
        }
        return feedRepository.reportPostAsExpired(
            postId = postId,
            postTitle = postTitle,
            authorId = authorId,
            currentUserId = currentUserId,
            currentUserName = currentUserName
        )
    }
}
