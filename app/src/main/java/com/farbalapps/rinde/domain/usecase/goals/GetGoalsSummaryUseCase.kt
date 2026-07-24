package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.repository.GoalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Representa el resumen agregado calculado a partir de todas las metas activas del usuario.
 *
 * @property totalSaved Monto total acumulado de todas las metas.
 * @property totalTarget Monto total de los objetivos combinados.
 * @property progressPercent Porcentaje global de avance global de ahorro (0-100).
 * @property monthlyGrowthPercent Porcentaje de crecimiento mensual respecto al snapshot anterior.
 */
data class GoalsSummary(
    val totalSaved: Double,
    val totalTarget: Double,
    val progressPercent: Int,
    val monthlyGrowthPercent: Int
)

/**
 * Caso de uso para obtener y calcular el resumen consolidado de las metas de ahorro.
 */
class GetGoalsSummaryUseCase @Inject constructor(
    private val repository: GoalsRepository
) {
    operator fun invoke(): Flow<GoalsSummary> {
        return repository.getGoals().map { goals ->
            if (goals.isEmpty()) {
                return@map GoalsSummary(
                    totalSaved = 0.0,
                    totalTarget = 0.0,
                    progressPercent = 0,
                    monthlyGrowthPercent = 0
                )
            }

            val totalSaved = goals.sumOf { it.currentAmount }
            val totalTarget = goals.sumOf { it.targetAmount }
            
            val progressPercent = if (totalTarget > 0) {
                ((totalSaved / totalTarget) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            val previousSnapshotTotal = goals.sumOf { it.monthlySnapshotAmount }
            val monthlyGrowthPercent = if (previousSnapshotTotal > 0) {
                val delta = totalSaved - previousSnapshotTotal
                ((delta / previousSnapshotTotal) * 100).toInt()
            } else {
                0
            }

            GoalsSummary(
                totalSaved = totalSaved,
                totalTarget = totalTarget,
                progressPercent = progressPercent,
                monthlyGrowthPercent = monthlyGrowthPercent
            )
        }
    }
}
