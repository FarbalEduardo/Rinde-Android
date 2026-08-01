package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.GoalTransaction
import com.farbalapps.rinde.domain.repository.GoalsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener el historial de transacciones (abonos/retiros) de una meta.
 */
class GetGoalTransactionsUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    operator fun invoke(goalId: String): Flow<List<GoalTransaction>> {
        return repository.getTransactionsForGoal(goalId)
    }
}
