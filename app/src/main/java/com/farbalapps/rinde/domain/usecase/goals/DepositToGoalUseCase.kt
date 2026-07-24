package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.repository.GoalsRepository
import javax.inject.Inject

/**
 * Caso de uso para registrar un depósito monetario a una meta específica.
 * Implementa la Opción B para depósitos excedentes que superan el monto objetivo.
 */
class DepositToGoalUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    suspend operator fun invoke(
        goalId: String,
        amount: Double,
        note: String,
        forceDeposit: Boolean = false
    ): Result<DepositResult> {
        if (amount <= 0) {
            return Result.failure(IllegalArgumentException("El monto debe ser mayor a cero"))
        }

        val goals = repository.getGoalsSnapshot()
        val goal = goals.find { it.id == goalId }
            ?: return Result.failure(NoSuchElementException("La meta especificada no existe"))

        val remaining = goal.targetAmount - goal.currentAmount
        if (amount > remaining && !forceDeposit) {
            // E3.4 Opción B: requiere confirmación para proceder con exceso
            return Result.success(DepositResult.RequiresConfirmation(excess = amount - remaining))
        }

        return runCatching {
            repository.depositToGoal(goalId, amount, note)
            val updatedGoals = repository.getGoalsSnapshot()
            val updatedGoal = updatedGoals.find { it.id == goalId }
            val completed = updatedGoal != null && updatedGoal.currentAmount >= updatedGoal.targetAmount
            DepositResult.Success(completed = completed)
        }
    }
}

/**
 * Representación del resultado de la acción de depósito.
 */
sealed interface DepositResult {
    /**
     * El depósito se realizó con éxito.
     * @property completed True si la meta alcanzó el 100% de su progreso.
     */
    data class Success(val completed: Boolean) : DepositResult

    /**
     * Se requiere confirmación explícita del usuario porque el depósito supera el remanente.
     * @property excess La cantidad de dinero que excede el objetivo original.
     */
    data class RequiresConfirmation(val excess: Double) : DepositResult
}
