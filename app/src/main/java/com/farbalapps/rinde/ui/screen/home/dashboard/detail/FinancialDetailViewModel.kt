package com.farbalapps.rinde.ui.screen.home.dashboard.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.FinancialPeriod
import com.farbalapps.rinde.domain.model.PeriodType
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.model.MonthlyFinancialRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

private data class PeriodContext(
    val period: FinancialPeriod,
    val extraExpenses: List<ExtraExpense>,
    val monthlyRecord: MonthlyFinancialRecord?
)

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
            dashboardRepository.checkAndPerformMonthlyRollover()
            goalsRepository.syncGoals()
        }
    }

    // Flujo reactivo que recarga los gastos del periodo:
    // Si es un Mes, filtra estrictamente por año y mes para independizar cada periodo.
    // De lo contrario, filtra por rango de timestamps.
    private val periodExpensesFlow = _selectedPeriod.flatMapLatest { period ->
        if (period is FinancialPeriod.Month) {
            dashboardRepository.getExtraExpenses(period.year, period.month)
        } else {
            dashboardRepository.getExtraExpensesBetween(period.startTimestamp, period.endTimestamp)
        }
    }

    private val monthlyRecordFlow = _selectedPeriod.flatMapLatest { period ->
        if (period is FinancialPeriod.Month) {
            dashboardRepository.getMonthlyRecord(period.year, period.month)
        } else {
            flowOf(null)
        }
    }

    private val periodContextFlow = combine(
        _selectedPeriod,
        periodExpensesFlow,
        monthlyRecordFlow
    ) { period, expenses, record ->
        PeriodContext(period, expenses, record)
    }

    private val combinedDataFlow = combine(
        periodContextFlow,
        dashboardRepository.getFinancialProfile(),
        listRepository.getItems(),
        goalsRepository.getGoals()
    ) { context, profile, shoppingItems, goals ->
        val period = context.period
        val extraExpenses = context.extraExpenses
        val monthlyRecord = context.monthlyRecord

        val cal = Calendar.getInstance()
        val nowYear = cal.get(Calendar.YEAR)
        val nowMonth = cal.get(Calendar.MONTH) + 1

        val isCurrentMonth = (period is FinancialPeriod.Month && period.year == nowYear && period.month == nowMonth)
        val isPastMonth = (period is FinancialPeriod.Month && (period.year < nowYear || (period.year == nowYear && period.month < nowMonth)))

        // Filtrar estrictamente los artículos marcados como comprados (isCompleted = true)
        val boughtItems = shoppingItems.filter { it.isCompleted }

        val periodIncome: Double
        val listTotal: Double
        val extraTotal: Double
        val goalsCommitted: Double
        val displayShoppingItems: List<com.farbalapps.rinde.domain.model.ShoppingItem>
        val activeGoals: List<com.farbalapps.rinde.domain.model.SavingsGoal>

        if (isCurrentMonth) {
            periodIncome = period.calculatePeriodIncome(profile)
            displayShoppingItems = boughtItems
            listTotal = boughtItems.sumOf { (it.price ?: 0.0) * it.quantity }
            extraTotal = extraExpenses.sumOf { it.amount }
            activeGoals = goals.filter { !it.isArchived }
            goalsCommitted = activeGoals.sumOf { goal ->
                val delta = goal.currentAmount - goal.monthlySnapshotAmount
                if (delta > 0.0) delta else 0.0
            }
        } else if (isPastMonth) {
            periodIncome = if (monthlyRecord != null && monthlyRecord.income > 0.0) {
                monthlyRecord.income
            } else {
                period.calculatePeriodIncome(profile)
            }
            extraTotal = if (extraExpenses.isNotEmpty()) {
                extraExpenses.sumOf { it.amount }
            } else {
                monthlyRecord?.extraExpensesTotal ?: 0.0
            }
            listTotal = monthlyRecord?.listTotal ?: 0.0
            goalsCommitted = monthlyRecord?.goalsCommittedTotal ?: 0.0
            displayShoppingItems = emptyList() // En meses pasados la lista activa no aplica
            activeGoals = emptyList()
        } else {
            // Day, Week, Fortnight, Custom
            periodIncome = period.calculatePeriodIncome(profile)
            displayShoppingItems = boughtItems
            val totalMonthlyBought = boughtItems.sumOf { (it.price ?: 0.0) * it.quantity }
            listTotal = (totalMonthlyBought / 30.0) * period.durationDays
            extraTotal = extraExpenses.sumOf { it.amount }
            activeGoals = goals.filter { !it.isArchived }
            val monthlyGoalsSavings = activeGoals.sumOf { goal ->
                val delta = goal.currentAmount - goal.monthlySnapshotAmount
                if (delta > 0.0) delta else 0.0
            }
            goalsCommitted = (monthlyGoalsSavings / 30.0) * period.durationDays
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
            shoppingItems = displayShoppingItems,
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
        val cal = Calendar.getInstance()
        val nowYear = cal.get(Calendar.YEAR)
        val nowMonth = cal.get(Calendar.MONTH) + 1
        val currentPeriod = _selectedPeriod.value
        val canNavigate = when (currentPeriod) {
            is FinancialPeriod.Month -> currentPeriod.year < nowYear || (currentPeriod.year == nowYear && currentPeriod.month < nowMonth)
            is FinancialPeriod.Fortnight -> {
                val nowDay = cal.get(Calendar.DAY_OF_MONTH)
                if (currentPeriod.year < nowYear) true
                else if (currentPeriod.year == nowYear && currentPeriod.month < nowMonth) true
                else if (currentPeriod.year == nowYear && currentPeriod.month == nowMonth) currentPeriod.isFirstHalf && nowDay > 15
                else false
            }
            else -> currentPeriod.endTimestamp < System.currentTimeMillis()
        }

        if (canNavigate) {
            _selectedPeriod.value = currentPeriod.next()
        }
    }

    fun onDateSelected(dateMillis: Long) {
        val now = System.currentTimeMillis()
        val targetMillis = if (dateMillis > now) now else dateMillis
        val cal = Calendar.getInstance().apply { timeInMillis = targetMillis }
        val updatedPeriod = when (_selectedPeriod.value.type) {
            PeriodType.DAY -> FinancialPeriod.Day(targetMillis)
            PeriodType.MONTH -> FinancialPeriod.Month(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            PeriodType.FORTNIGHT -> {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                FinancialPeriod.Fortnight(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, day <= 15)
            }
            PeriodType.WEEK -> FinancialPeriod.Week(targetMillis)
            PeriodType.CUSTOM -> FinancialPeriod.Day(targetMillis)
        }
        _selectedPeriod.value = updatedPeriod
        _showDatePicker.value = false
    }

    fun onCustomRangeSelected(startMillis: Long, endMillis: Long) {
        val now = System.currentTimeMillis()
        val validStart = if (startMillis > now) now else startMillis
        val validEnd = if (endMillis > now) now else endMillis
        _selectedPeriod.value = FinancialPeriod.Custom(validStart, validEnd)
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
