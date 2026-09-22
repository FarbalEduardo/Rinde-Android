package com.farbalapps.rinde.ui.screen.home.dashboard

import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.ExtraIncome
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.SavingsGoal

enum class FinancialHealthStatus {
    GOOD,
    WARNING,
    CRITICAL,
    SETUP_REQUIRED
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val currentMonthName: String = "",
    val currentMonth: Int = 1,
    val currentYear: Int = 2026,
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
    val currency: String = "MXN",
    val isIncomeSheetOpen: Boolean = false,
    val isExpenseSheetOpen: Boolean = false,
    val editingExpense: ExtraExpense? = null,
    val isEditExpenseSheetOpen: Boolean = false,
    val isExtraIncomeSheetOpen: Boolean = false,
    val editingExtraIncome: ExtraIncome? = null,
    val isEditExtraIncomeSheetOpen: Boolean = false,
    val isFinancialCalendarOpen: Boolean = false,
    val pendingDeleteExpense: ExtraExpense? = null,
    val pendingDeleteIncome: ExtraIncome? = null
) {
    val needsMonthlyIncomeCapture: Boolean
        get() = isVariableIncome && baseMonthlyIncome <= 0.0

    val totalExpenses: Double
        get() = listTotal + extraExpensesTotal + goalsCommittedTotal

    val availableAmount: Double
        get() = monthlyIncome - totalExpenses

    val expenseFraction: Float
        get() = if (monthlyIncome <= 0.0) 0f else (totalExpenses / monthlyIncome).coerceIn(0.0, 1.0).toFloat()

    val freeFraction: Float
        get() = (1f - expenseFraction).coerceIn(0f, 1f)

    val expensePercentage: Int
        get() = (expenseFraction * 100).toInt()

    val freePercentage: Int
        get() = (freeFraction * 100).toInt()

    val healthStatus: FinancialHealthStatus
        get() = when {
            availableAmount < 0.0 -> FinancialHealthStatus.CRITICAL
            !hasIncomeConfigured && totalExpenses == 0.0 -> FinancialHealthStatus.SETUP_REQUIRED
            monthlyIncome <= 0.0 -> FinancialHealthStatus.CRITICAL
            freeFraction < 0.15f -> FinancialHealthStatus.CRITICAL
            freeFraction < 0.35f -> FinancialHealthStatus.WARNING
            else -> FinancialHealthStatus.GOOD
        }

    val hasIncomeConfigured: Boolean
        get() = if (isVariableIncome) profile != null else (profile != null && profile.income > 0.0)
}
