package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.repository.GoalsRepository
import javax.inject.Inject

/**
 * Caso de uso para actualizar los detalles de una meta (título, monto, icono, color, fecha límite).
 */
class UpdateGoalUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    suspend operator fun invoke(goal: SavingsGoal): Result<Unit> {
        if (goal.title.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre es requerido"))
        }
        if (goal.title.length > 30) {
            return Result.failure(IllegalArgumentException("El título no puede superar los 30 caracteres"))
        }
        if (goal.targetAmount <= 0) {
            return Result.failure(IllegalArgumentException("El monto objetivo debe ser mayor a cero"))
        }
        return runCatching {
            repository.updateGoal(goal)
        }
    }
}
