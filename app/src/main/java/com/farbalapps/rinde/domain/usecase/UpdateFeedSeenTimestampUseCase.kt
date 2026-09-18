package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * UseCase de estado de lectura: persiste el timestamp en que el usuario vio el feed
 * por última vez, permitiendo detectar si hay publicaciones nuevas al reanudar la app.
 *
 * Desacopla al ViewModel del SyncMetadataDao manteniendo la capa de dominio 100% pura.
 *
 * [HU-01] Permite que CommunityViewModel muestre el banner "N nuevas publicaciones".
 *
 * @param feedRepository Repositorio que persiste el timestamp en la capa de datos.
 */
class UpdateFeedSeenTimestampUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    /** Marca el momento actual como el último timestamp de lectura del feed. */
    suspend operator fun invoke() = feedRepository.updateFeedSeenTimestamp()
}
