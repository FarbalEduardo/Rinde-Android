package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.repository.GoalsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener una meta específica por su ID.
 * Retorna un flujo reactivo para observar cambios en tiempo real.
 */
class GetGoalByIdUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    operator fun invoke(goalId: String): Flow<SavingsGoal?> {
        return repository.getGoalById(goalId)
    }
}
