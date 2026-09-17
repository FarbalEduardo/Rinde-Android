package com.farbalapps.rinde.ui.screen.home.dashboard.detail

import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.FinancialPeriod
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.PeriodType
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.ui.screen.home.dashboard.FinancialHealthStatus

/**
 * Estado UI para la pantalla de Detalle Financiero por periodos.
 */
data class FinancialDetailUiState(
    val isLoading: Boolean = false,
    val selectedPeriod: FinancialPeriod = FinancialPeriod.currentMonth(),
    val periodType: PeriodType = PeriodType.MONTH,
    val profile: FinancialProfile? = null,
    val periodIncome: Double = 0.0,
    val listTotal: Double = 0.0,
    val extraExpensesTotal: Double = 0.0,
    val goalsCommittedTotal: Double = 0.0,
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val extraExpenses: List<ExtraExpense> = emptyList(),
    val activeGoals: List<SavingsGoal> = emptyList(),
    val currency: String = "MXN",
    val showDatePickerModal: Boolean = false,
    val showDateRangePickerModal: Boolean = false,
    val editingExpense: ExtraExpense? = null,
    val isEditExpenseSheetOpen: Boolean = false
) {
    val totalExpenses: Double
        get() = listTotal + extraExpensesTotal + goalsCommittedTotal

    val availableAmount: Double
        get() = periodIncome - totalExpenses

    val healthStatus: FinancialHealthStatus
        get() = when {
            periodIncome <= 0.0 -> FinancialHealthStatus.CRITICAL
            availableAmount < 0.0 -> FinancialHealthStatus.CRITICAL
            (totalExpenses / periodIncome) > 0.85 -> FinancialHealthStatus.WARNING
            else -> FinancialHealthStatus.GOOD
        }

    val expenseFraction: Float
        get() = if (periodIncome > 0) {
            (totalExpenses / periodIncome).coerceIn(0.0, 1.0).toFloat()
        } else 0f

    val expensePercentage: Int
        get() = (expenseFraction * 100).toInt()

    val freePercentage: Int
        get() = (100 - expensePercentage).coerceAtLeast(0)
}
