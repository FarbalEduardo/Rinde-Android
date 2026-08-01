package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.repository.GoalsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Caso de uso para obtener el historial de metas archivadas/cumplidas del usuario.
 */
class GetArchivedGoalsUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    operator fun invoke(): Flow<List<SavingsGoal>> {
        return repository.getArchivedGoals()
    }
}
