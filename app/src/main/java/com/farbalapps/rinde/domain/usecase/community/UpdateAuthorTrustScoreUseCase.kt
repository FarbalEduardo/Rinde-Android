package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.domain.repository.FeedRepository
import javax.inject.Inject

/**
 * Caso de uso para recalcular el nivel de confianza y reputación del autor de una oferta.
 * Sigue Clean Architecture delegando el acceso a datos y persistencia en [FeedRepository].
 */
class UpdateAuthorTrustScoreUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(authorId: String): Result<Unit> {
        return feedRepository.recalculateAuthorTrustScore(authorId)
    }
}
