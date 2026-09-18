package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * UseCase de mantenimiento: elimina posts del caché Room con más de [maxAgeDays] días,
 * preservando los guardados (`isSavedByMe = 1`) y los posts Hot (`votesScore >= 2`).
 *
 * Desacopla al ViewModel del PostDao manteniendo la capa de dominio 100% pura.
 *
 * [HU-01] Garantiza que el feed local no crezca indefinidamente en disco del usuario.
 *
 * @param feedRepository Repositorio que expone la operación de limpieza de caché.
 */
class CleanOldCacheUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    /**
     * Ejecuta la limpieza de caché.
     *
     * @param maxAgeDays Días máximos de retención de un post en caché. Default: 15.
     * @return [Result.success] si la operación fue exitosa, [Result.failure] en caso contrario.
     */
    suspend operator fun invoke(maxAgeDays: Int = 15): Result<Unit> =
        feedRepository.deleteOldCachedPosts(
            thresholdMs = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1_000L)
        )
}
