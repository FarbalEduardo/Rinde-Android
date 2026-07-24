package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.repository.GoalsRepository
import javax.inject.Inject

/**
 * Caso de uso para la creación de una nueva meta de ahorro.
 * Controla las reglas de negocio de entrada de datos y el límite máximo de 3 metas.
 */
class CreateGoalUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    companion object {
        const val FREE_TIER_LIMIT = 3
    }

    suspend operator fun invoke(goal: SavingsGoal): Result<Unit> {
        val currentGoals = repository.getGoalsSnapshot()
        if (currentGoals.size >= FREE_TIER_LIMIT) {
            return Result.failure(IllegalStateException("Ya tienes 3 metas activas. Elimina una para agregar otra."))
        }
        if (goal.title.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre es requerido"))
        }
        if (goal.title.length > 30) {
            return Result.failure(IllegalArgumentException("El título no puede superar los 30 caracteres"))
        }
        if (goal.targetAmount <= 0) {
            return Result.failure(IllegalArgumentException("El monto debe ser mayor a cero"))
        }
        return runCatching {
            repository.createGoal(goal)
        }
    }
}
