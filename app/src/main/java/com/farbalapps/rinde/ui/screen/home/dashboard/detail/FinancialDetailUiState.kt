package com.farbalapps.rinde.ui.screen.home.dashboard.detail

import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.ExtraIncome
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
    val basePeriodIncome: Double = 0.0,
    val extraIncomesTotal: Double = 0.0,
    val proportionalExtraIncome: Double = 0.0,
    val listTotal: Double = 0.0,
    val extraExpensesTotal: Double = 0.0,
    val goalsCommittedTotal: Double = 0.0,
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val extraExpenses: List<ExtraExpense> = emptyList(),
    val extraIncomes: List<ExtraIncome> = emptyList(),
    val activeGoals: List<SavingsGoal> = emptyList(),
    val currency: String = "MXN",
    val showDatePickerModal: Boolean = false,
    val showDateRangePickerModal: Boolean = false,
    val editingExpense: ExtraExpense? = null,
    val isEditExpenseSheetOpen: Boolean = false,
    val isAddExpenseSheetOpen: Boolean = false,
    val isFinancialCalendarOpen: Boolean = false,
    val calendarSelectedDateMillis: Long? = null,
    val isIncomeSheetOpen: Boolean = false,
    val isDeleteSalaryDialogOpen: Boolean = false,
    val pendingDeleteSalaryYear: Int = 0,
    val pendingDeleteSalaryMonth: Int = 0,
    val editingIncome: ExtraIncome? = null,
    val isEditIncomeSheetOpen: Boolean = false,
    val isAddIncomeSheetOpen: Boolean = false
) {
    val isVariableIncome: Boolean
        get() = profile?.isVariableIncome ?: false
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

    val canNavigateNext: Boolean
        get() {
            val cal = java.util.Calendar.getInstance()
            val nowYear = cal.get(java.util.Calendar.YEAR)
            val nowMonth = cal.get(java.util.Calendar.MONTH) + 1
            return when (val period = selectedPeriod) {
                is FinancialPeriod.Month -> period.year < nowYear || (period.year == nowYear && period.month < nowMonth)
                is FinancialPeriod.Fortnight -> {
                    val nowDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    if (period.year < nowYear) true
                    else if (period.year == nowYear && period.month < nowMonth) true
                    else if (period.year == nowYear && period.month == nowMonth) period.isFirstHalf && nowDay > 15
                    else false
                }
                else -> period.endTimestamp < System.currentTimeMillis()
            }
        }
}
