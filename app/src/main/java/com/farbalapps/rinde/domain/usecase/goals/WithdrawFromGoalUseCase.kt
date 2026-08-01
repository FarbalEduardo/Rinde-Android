package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.repository.GoalsRepository
import javax.inject.Inject

/**
 * Caso de uso para retirar dinero de una meta (abono negativo).
 */
class WithdrawFromGoalUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    suspend operator fun invoke(
        goalId: String,
        amount: Double,
        note: String
    ): Result<Unit> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("El monto a retirar debe ser mayor a cero"))
        }

        val goals = repository.getGoalsSnapshot()
        val goal = goals.find { it.id == goalId }
            ?: return Result.failure(NoSuchElementException("La meta especificada no existe"))

        if (amount > goal.currentAmount) {
            return Result.failure(IllegalArgumentException("No puedes retirar más del monto ahorrado actualmente"))
        }

        return runCatching {
            repository.withdrawFromGoal(goalId, amount, note)
        }
    }
}
