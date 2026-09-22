package com.farbalapps.rinde.domain.usecase.dashboard

import com.farbalapps.rinde.domain.model.DashboardFinancialSummary
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Caso de uso para obtener el resumen financiero reactivo del Dashboard.
 * Consolida perfil de ingresos, sueldos variables, ganancias extras, gastos del hogar,
 * artículos comprados de la lista de compras y metas de ahorro activas.
 */
class GetDashboardSummaryUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val listRepository: ListRepository,
    private val goalsRepository: GoalsRepository
) {
    operator fun invoke(year: Int, month: Int): Flow<DashboardFinancialSummary> {
        val incomeDataFlow = combine(
            dashboardRepository.getFinancialProfile(),
            dashboardRepository.getMonthlyVariableIncome(year, month),
            dashboardRepository.getExtraIncomes(year, month)
        ) { profile, variableIncome, extraIncomes ->
            Triple(profile, variableIncome, extraIncomes)
        }

        return combine(
            incomeDataFlow,
            listRepository.getItems(),
            goalsRepository.getGoals(),
            dashboardRepository.getExtraExpenses(year, month)
        ) { (profile, variableIncome, extraIncomes), shoppingItems, goals, extraExpenses ->

            val isVariable = profile?.isVariableIncome ?: false
            val baseMonthlyIncome = if (isVariable) {
                variableIncome ?: 0.0
            } else {
                profile?.monthlyEquivalent ?: 0.0
            }

            val extraIncomesTotal = extraIncomes.sumOf { it.amount }
            val monthlyIncome = baseMonthlyIncome + extraIncomesTotal

            // Mi Lista: únicamente artículos marcados como comprados (isCompleted = true)
            val boughtItems = shoppingItems.filter { it.isCompleted }
            val listTotal = boughtItems.sumOf { item ->
                (item.price ?: 0.0) * item.quantity
            }

            // Gastos Extra del Hogar (Luz, Agua, Renta, etc.)
            val extraTotal = extraExpenses.sumOf { it.amount }

            // Metas activas: no archivadas
            val activeGoals = goals.filter { !it.isArchived }

            // Total comprometido/ahorrado en metas este mes
            val monthlyGoalsSavings = activeGoals.sumOf { goal ->
                val delta = goal.currentAmount - goal.monthlySnapshotAmount
                if (delta > 0.0) delta else 0.0
            }

            DashboardFinancialSummary(
                profile = profile,
                isVariableIncome = isVariable,
                baseMonthlyIncome = baseMonthlyIncome,
                monthlyIncome = monthlyIncome,
                listTotal = listTotal,
                extraExpensesTotal = extraTotal,
                extraIncomesTotal = extraIncomesTotal,
                goalsCommittedTotal = monthlyGoalsSavings,
                extraExpenses = extraExpenses,
                extraIncomes = extraIncomes,
                activeGoals = activeGoals,
                currency = profile?.currency ?: "MXN"
            )
        }
    }
}
