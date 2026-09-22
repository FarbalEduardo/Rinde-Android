package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * Caso de uso responsable de alternar el estado de guardado/favorito de una publicación en la comunidad.
 *
 * @property feedRepository Repositorio de ofertas y feed comunitario.
 */
class ToggleSavePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    /**
     * Ejecuta el guardado o desguardado de una oferta para un usuario determinado.
     *
     * @param userId Identificador único del usuario que realiza la acción.
     * @param postId Identificador único de la publicación a guardar/desguardar.
     * @return [Result.success] con [Unit] si la operación fue exitosa, o [Result.failure] con la excepción correspondiente.
     */
    suspend operator fun invoke(userId: String, postId: String): Result<Unit> {
        if (userId.isBlank() || postId.isBlank()) {
            return Result.failure(IllegalArgumentException("userId and postId must not be blank"))
        }
        return feedRepository.toggleSave(userId, postId)
    }
}
