package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.repository.GoalsRepository
import javax.inject.Inject

/**
 * Caso de uso para reactivar o desarchivar una meta de ahorro.
 * Controla el límite máximo de metas activas permitidas (2 metas).
 */
class UnarchiveGoalUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    suspend operator fun invoke(goalId: String): Result<Unit> {
        val currentActiveGoals = repository.getGoalsSnapshot()
        if (currentActiveGoals.size >= CreateGoalUseCase.FREE_TIER_LIMIT) {
            return Result.failure(
                IllegalStateException("No puedes reactivar esta meta porque ya tienes 2 metas activas. Elimina o archiva una meta activa primero.")
            )
        }
        return runCatching {
            repository.unarchiveGoal(goalId)
        }
    }
}
