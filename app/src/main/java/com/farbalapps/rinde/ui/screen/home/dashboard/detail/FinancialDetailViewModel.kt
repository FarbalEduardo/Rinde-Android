package com.farbalapps.rinde.ui.screen.home.dashboard.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.FinancialPeriod
import com.farbalapps.rinde.domain.model.PeriodType
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FinancialDetailViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val listRepository: ListRepository,
    private val goalsRepository: GoalsRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow<FinancialPeriod>(FinancialPeriod.currentMonth())
    private val _showDatePicker = MutableStateFlow(false)
    private val _showDateRangePicker = MutableStateFlow(false)
    private val _editingExpense = MutableStateFlow<ExtraExpense?>(null)

    init {
        viewModelScope.launch {
            dashboardRepository.syncFromFirebase()
        }
    }

    // Flujo reactivo que recarga los gastos del periodo cada vez que cambia el rango de fechas
    private val periodExpensesFlow = _selectedPeriod.flatMapLatest { period ->
        dashboardRepository.getExtraExpensesBetween(period.startTimestamp, period.endTimestamp)
    }

    private val combinedDataFlow = combine(
        _selectedPeriod,
        dashboardRepository.getFinancialProfile(),
        listRepository.getItems(),
        goalsRepository.getGoals(),
        periodExpensesFlow
    ) { period, profile, shoppingItems, goals, extraExpenses ->

        val periodIncome = period.calculatePeriodIncome(profile)

        // Mi Lista: base mensual normalizada al periodo
        val totalListMonthly = shoppingItems.sumOf { (it.price ?: 0.0) * it.quantity }
        val listTotal = if (period is FinancialPeriod.Month) {
            totalListMonthly
        } else {
            (totalListMonthly / 30.0) * period.durationDays
        }

        // Gastos del hogar del periodo
        val extraTotal = extraExpenses.sumOf { it.amount }

        // Metas activas
        val activeGoals = goals.filter { !it.isCompleted && !it.isArchived }
        val monthlyGoalsSavings = activeGoals.sumOf { goal ->
            val delta = goal.currentAmount - goal.monthlySnapshotAmount
            if (delta > 0.0) delta else 0.0
        }
        val goalsCommitted = if (period is FinancialPeriod.Month) {
            monthlyGoalsSavings
        } else {
            (monthlyGoalsSavings / 30.0) * period.durationDays
        }

        FinancialDetailUiState(
            isLoading = false,
            selectedPeriod = period,
            periodType = period.type,
            profile = profile,
            periodIncome = periodIncome,
            listTotal = listTotal,
            extraExpensesTotal = extraTotal,
            goalsCommittedTotal = goalsCommitted,
            shoppingItems = shoppingItems,
            extraExpenses = extraExpenses,
            activeGoals = activeGoals,
            currency = profile?.currency ?: "MXN"
        )
    }

    val uiState: StateFlow<FinancialDetailUiState> = combine(
        combinedDataFlow,
        _showDatePicker,
        _showDateRangePicker,
        _editingExpense
    ) { state, showDate, showRange, editingExpense ->
        state.copy(
            showDatePickerModal = showDate,
            showDateRangePickerModal = showRange,
            editingExpense = editingExpense,
            isEditExpenseSheetOpen = editingExpense != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialDetailUiState()
    )

    fun setPeriodType(type: PeriodType) {
        val newPeriod: FinancialPeriod = when (type) {
            PeriodType.MONTH -> FinancialPeriod.currentMonth()
            PeriodType.FORTNIGHT -> FinancialPeriod.currentFortnight()
            PeriodType.WEEK -> FinancialPeriod.currentWeek()
            PeriodType.DAY -> FinancialPeriod.today()
            PeriodType.CUSTOM -> {
                _showDateRangePicker.value = true
                _selectedPeriod.value
            }
        }
        _selectedPeriod.value = newPeriod
    }

    fun navigatePrevious() {
        _selectedPeriod.value = _selectedPeriod.value.previous()
    }

    fun navigateNext() {
        _selectedPeriod.value = _selectedPeriod.value.next()
    }

    fun onDateSelected(dateMillis: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val updatedPeriod = when (_selectedPeriod.value.type) {
            PeriodType.DAY -> FinancialPeriod.Day(dateMillis)
            PeriodType.MONTH -> FinancialPeriod.Month(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            PeriodType.FORTNIGHT -> {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                FinancialPeriod.Fortnight(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, day <= 15)
            }
            PeriodType.WEEK -> FinancialPeriod.Week(dateMillis)
            PeriodType.CUSTOM -> FinancialPeriod.Day(dateMillis)
        }
        _selectedPeriod.value = updatedPeriod
        _showDatePicker.value = false
    }

    fun onCustomRangeSelected(startMillis: Long, endMillis: Long) {
        _selectedPeriod.value = FinancialPeriod.Custom(startMillis, endMillis)
        _showDateRangePicker.value = false
    }

    fun openDatePicker() {
        if (_selectedPeriod.value is FinancialPeriod.Custom) {
            _showDateRangePicker.value = true
        } else {
            _showDatePicker.value = true
        }
    }

    fun closeDatePicker() {
        _showDatePicker.value = false
    }

    fun openDateRangePicker() {
        _showDateRangePicker.value = true
    }

    fun closeDateRangePicker() {
        _showDateRangePicker.value = false
    }

    fun openEditExpense(expense: ExtraExpense) {
        _editingExpense.value = expense
    }

    fun closeEditExpense() {
        _editingExpense.value = null
    }

    fun updateExtraExpense(id: String, label: String, amount: Double) {
        if (label.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            dashboardRepository.updateExtraExpense(id, label, amount)
            closeEditExpense()
        }
    }

    fun deleteExtraExpense(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteExtraExpense(id)
            if (_editingExpense.value?.id == id) {
                closeEditExpense()
            }
        }
    }
}
