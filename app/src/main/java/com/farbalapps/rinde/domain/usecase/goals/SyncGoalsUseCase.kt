package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.repository.GoalsRepository
import javax.inject.Inject

/**
 * Caso de uso para desencadenar la sincronización en segundo plano de metas de ahorro con Firestore.
 */
class SyncGoalsUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    suspend operator fun invoke() {
        runCatching {
            repository.syncGoals()
        }
    }
}
