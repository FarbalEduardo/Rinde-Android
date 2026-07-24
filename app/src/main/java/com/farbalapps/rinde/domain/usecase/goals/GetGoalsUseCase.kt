package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.repository.GoalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Caso de uso para obtener el listado de metas de ahorro ordenadas.
 * La que tenga mayor cantidad ahorrada (currentAmount) se prioriza primero.
 */
class GetGoalsUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    operator fun invoke(): Flow<List<SavingsGoal>> {
        return repository.getGoals().map { list ->
            list.sortedByDescending { it.currentAmount }
        }
    }
}
