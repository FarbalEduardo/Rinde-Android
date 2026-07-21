package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.repository.GoalsRepository
import javax.inject.Inject

/**
 * Caso de uso para eliminar una meta de ahorro.
 */
class DeleteGoalUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    suspend operator fun invoke(goalId: String): Result<Unit> {
        return runCatching {
            repository.deleteGoal(goalId)
        }
    }
}
