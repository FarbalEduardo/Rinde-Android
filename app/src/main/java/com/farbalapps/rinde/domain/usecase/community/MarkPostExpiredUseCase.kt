package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * Caso de uso responsable de actualizar el estado de vigencia o expiración de una publicación.
 *
 * @property feedRepository Repositorio de ofertas y feed comunitario.
 */
class MarkPostExpiredUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    /**
     * Marca un post como expirado tanto en el overlay reactivo local como en el backend.
     *
     * @param postId Identificador del post.
     * @return [Result] con el resultado de la operación remota.
     */
    suspend fun markExpired(postId: String): Result<Unit> {
        if (postId.isBlank()) return Result.failure(IllegalArgumentException("postId must not be blank"))
        feedRepository.updatePostStatusLocal(postId, VerificationStatus.EXPIRED)
        return feedRepository.markPostAsExpired(postId)
    }

    /**
     * Restablece un post a estado disponible/pendiente tanto localmente como en el backend.
     *
     * @param postId Identificador del post.
     * @return [Result] con el resultado de la operación remota.
     */
    suspend fun markAvailable(postId: String): Result<Unit> {
        if (postId.isBlank()) return Result.failure(IllegalArgumentException("postId must not be blank"))
        feedRepository.updatePostStatusLocal(postId, VerificationStatus.PENDING)
        return feedRepository.markPostAsAvailable(postId)
    }
}
