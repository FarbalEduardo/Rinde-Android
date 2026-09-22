package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * Caso de uso responsable de eliminar una publicación y sus recursos multimedia asociados.
 *
 * @property feedRepository Repositorio de ofertas y feed comunitario.
 */
class DeletePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    /**
     * Elimina el documento del post y solicita la limpieza de sus imágenes asociadas en el almacenamiento remoto.
     *
     * @param postId Identificador único de la publicación a eliminar.
     * @param photoUrls Lista de URLs remotas de las fotografías asociadas al post.
     * @return [Result.success] si se eliminó correctamente, o [Result.failure] en caso de error.
     */
    suspend operator fun invoke(postId: String, photoUrls: List<String>): Result<Unit> {
        if (postId.isBlank()) {
            return Result.failure(IllegalArgumentException("postId must not be blank"))
        }
        return feedRepository.deletePost(postId, photoUrls)
    }
}
