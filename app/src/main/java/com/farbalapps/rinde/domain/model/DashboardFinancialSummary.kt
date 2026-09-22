package com.farbalapps.rinde.domain.model

/**
 * Representa el resumen financiero consolidado para la pantalla principal del Dashboard.
 */
data class DashboardFinancialSummary(
    val profile: FinancialProfile? = null,
    val isVariableIncome: Boolean = false,
    val baseMonthlyIncome: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val listTotal: Double = 0.0,
    val extraExpensesTotal: Double = 0.0,
    val extraIncomesTotal: Double = 0.0,
    val goalsCommittedTotal: Double = 0.0,
    val extraExpenses: List<ExtraExpense> = emptyList(),
    val extraIncomes: List<ExtraIncome> = emptyList(),
    val activeGoals: List<SavingsGoal> = emptyList(),
    val currency: String = "MXN"
)
