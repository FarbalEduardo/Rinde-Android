package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import java.util.Calendar
import javax.inject.Inject

/**
 * Caso de uso para calcular la sugerencia de ahorro periódico.
 * Por defecto calcula una aportación mensual estimada basada en la fecha límite.
 */
class CalculateGoalSuggestionUseCase @Inject constructor() {
    
    /**
     * Retorna el monto sugerido por mes para alcanzar la meta a tiempo.
     * Si no hay fecha límite, o ya pasó la fecha, o ya se alcanzó la meta, retorna 0.0.
     */
    operator fun invoke(goal: SavingsGoal): Double {
        if (goal.isCompleted || goal.currentAmount >= goal.targetAmount) return 0.0
        val targetDate = goal.targetDate ?: return 0.0
        val now = System.currentTimeMillis()
        
        if (targetDate <= now) return 0.0

        val remainingAmount = goal.targetAmount - goal.currentAmount
        
        // Calcular diferencia en meses
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calTarget = Calendar.getInstance().apply { timeInMillis = targetDate }
        
        var monthsDiff = (calTarget.get(Calendar.YEAR) - calNow.get(Calendar.YEAR)) * 12 +
                calTarget.get(Calendar.MONTH) - calNow.get(Calendar.MONTH)
                
        // Si es el mismo mes o queda menos de un mes, sugerimos pagarlo todo este mes.
        if (monthsDiff <= 0) {
            monthsDiff = 1
        }
        
        return remainingAmount / monthsDiff
    }
}
