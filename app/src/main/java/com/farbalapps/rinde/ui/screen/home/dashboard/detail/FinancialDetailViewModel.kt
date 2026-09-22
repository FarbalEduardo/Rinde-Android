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

import com.farbalapps.rinde.domain.model.ExtraIncome

private data class PeriodContext(
    val period: FinancialPeriod,
    val extraExpenses: List<ExtraExpense>,
    val extraIncomes: List<ExtraIncome>,
    val monthlyRecord: MonthlyFinancialRecord?,
    val variableIncome: Double?
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
    private val _isAddExpenseSheetOpen = MutableStateFlow(false)
    private val _isFinancialCalendarOpen = MutableStateFlow(false)
    private val _calendarSelectedDate = MutableStateFlow<Long?>(null)
    private val _isIncomeSheetOpen = MutableStateFlow(false)
    private val _isDeleteSalaryDialogOpen = MutableStateFlow(false)
    private val _pendingDeleteSalaryYear = MutableStateFlow(0)
    private val _pendingDeleteSalaryMonth = MutableStateFlow(0)
    private val _editingIncome = MutableStateFlow<ExtraIncome?>(null)
    private val _isAddIncomeSheetOpen = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            dashboardRepository.syncFromFirebase()
            dashboardRepository.checkAndPerformMonthlyRollover()
            goalsRepository.syncGoals()
        }
    }

    // Flujo reactivo que recarga los gastos del periodo:
    // Si es un Mes, filtra estrictamente por año y mes para independizar cada periodo.
    // De lo contrario, filtra por rango de timestamps por expenseDate.
    private val periodExpensesFlow = _selectedPeriod.flatMapLatest { period ->
        if (period is FinancialPeriod.Month) {
            dashboardRepository.getExtraExpenses(period.year, period.month)
        } else {
            dashboardRepository.getExtraExpensesBetween(period.startTimestamp, period.endTimestamp)
        }
    }

    // Flujo reactivo que recarga los ingresos extras del mes del periodo:
    private val periodExtraIncomesFlow = _selectedPeriod.flatMapLatest { period ->
        val cal = Calendar.getInstance().apply { timeInMillis = period.startTimestamp }
        val year = if (period is FinancialPeriod.Month) period.year else cal.get(Calendar.YEAR)
        val month = if (period is FinancialPeriod.Month) period.month else (cal.get(Calendar.MONTH) + 1)
        dashboardRepository.getExtraIncomes(year, month)
    }

    private val monthlyRecordFlow = _selectedPeriod.flatMapLatest { period ->
        if (period is FinancialPeriod.Month) {
            dashboardRepository.getMonthlyRecord(period.year, period.month)
        } else {
            flowOf(null)
        }
    }

    // Flujo reactivo que recarga el sueldo variable capturado para el mes del periodo:
    private val periodVariableIncomeFlow = _selectedPeriod.flatMapLatest { period ->
        val cal = Calendar.getInstance().apply { timeInMillis = period.startTimestamp }
        val year = if (period is FinancialPeriod.Month) period.year else cal.get(Calendar.YEAR)
        val month = if (period is FinancialPeriod.Month) period.month else (cal.get(Calendar.MONTH) + 1)
        dashboardRepository.getMonthlyVariableIncome(year, month)
    }

    private val periodContextFlow = combine(
        _selectedPeriod,
        periodExpensesFlow,
        periodExtraIncomesFlow,
        monthlyRecordFlow,
        periodVariableIncomeFlow
    ) { period, expenses, incomes, record, variableIncome ->
        PeriodContext(period, expenses, incomes, record, variableIncome)
    }

    private val combinedDataFlow = combine(
        periodContextFlow,
        dashboardRepository.getFinancialProfile(),
        listRepository.getItems(),
        goalsRepository.getGoals()
    ) { context, profile, shoppingItems, goals ->
        val period = context.period
        val extraExpenses = context.extraExpenses
        val extraIncomes = context.extraIncomes
        val monthlyRecord = context.monthlyRecord
        val variableIncome = context.variableIncome

        val cal = Calendar.getInstance()
        val nowYear = cal.get(Calendar.YEAR)
        val nowMonth = cal.get(Calendar.MONTH) + 1

        val isCurrentMonth = (period is FinancialPeriod.Month && period.year == nowYear && period.month == nowMonth)
        val isPastMonth = (period is FinancialPeriod.Month && (period.year < nowYear || (period.year == nowYear && period.month < nowMonth)))
        val isFutureMonth = (period is FinancialPeriod.Month && (period.year > nowYear || (period.year == nowYear && period.month > nowMonth)))

        // Filtrar estrictamente los artículos marcados como comprados (isCompleted = true)
        val boughtItems = shoppingItems.filter { it.isCompleted }

        val isVariable = profile?.isVariableIncome ?: false
        val basePeriodIncome: Double
        val extraIncomesTotal = extraIncomes.sumOf { it.amount }
        val proportionalExtraIncome: Double
        val periodIncome: Double
        val listTotal: Double
        val extraTotal: Double
        val goalsCommitted: Double
        val displayShoppingItems: List<com.farbalapps.rinde.domain.model.ShoppingItem>
        val activeGoals: List<com.farbalapps.rinde.domain.model.SavingsGoal>

        // Comprobación de vigencia para ingresos fijos
        val profileStartDate = profile?.customStartDate ?: (profile?.updatedAt ?: 0L)
        val profileEndDate = profile?.customEndDate
        val isAfterProfileEnd = !isVariable && profileEndDate != null && profileEndDate > 0L && period.startTimestamp > profileEndDate

        if (isCurrentMonth) {
            basePeriodIncome = if (isAfterProfileEnd) {
                0.0
            } else if (monthlyRecord != null && monthlyRecord.isClosed && monthlyRecord.income <= 0.0) {
                0.0
            } else if (isVariable) {
                variableIncome ?: 0.0
            } else {
                period.calculatePeriodIncome(profile)
            }
            proportionalExtraIncome = extraIncomesTotal
            periodIncome = basePeriodIncome + proportionalExtraIncome
            displayShoppingItems = boughtItems
            listTotal = boughtItems.sumOf { (it.price ?: 0.0) * it.quantity }
            extraTotal = extraExpenses.sumOf { it.amount }
            activeGoals = goals.filter { !it.isArchived }
            goalsCommitted = activeGoals.sumOf { goal ->
                val delta = goal.currentAmount - goal.monthlySnapshotAmount
                if (delta > 0.0) delta else 0.0
            }
        } else if (isPastMonth) {
            val profileCal = Calendar.getInstance().apply {
                timeInMillis = if (profileStartDate > 0L) profileStartDate else System.currentTimeMillis()
            }
            val pStartYear = profileCal.get(Calendar.YEAR)
            val pStartMonth = profileCal.get(Calendar.MONTH) + 1

            // Solo aplica el sueldo fijo del perfil si el perfil fue configurado en o antes de este periodo
            val wasProfileActiveInPeriod = profile != null && profile.income > 0.0 && (
                profileStartDate <= 0L || pStartYear < period.year || (pStartYear == period.year && pStartMonth <= period.month)
            )

            basePeriodIncome = if (isAfterProfileEnd) {
                0.0
            } else if (monthlyRecord != null) {
                if (monthlyRecord.isClosed && monthlyRecord.income <= 0.0) 0.0
                else if (monthlyRecord.income > 0.0) monthlyRecord.income
                else if (isVariable) (variableIncome ?: 0.0)
                else if (wasProfileActiveInPeriod) period.calculatePeriodIncome(profile)
                else 0.0
            } else if (isVariable) {
                variableIncome ?: 0.0
            } else if (wasProfileActiveInPeriod) {
                period.calculatePeriodIncome(profile)
            } else {
                0.0
            }
            proportionalExtraIncome = extraIncomesTotal
            periodIncome = basePeriodIncome + proportionalExtraIncome
            extraTotal = if (extraExpenses.isNotEmpty()) {
                extraExpenses.sumOf { it.amount }
            } else {
                monthlyRecord?.extraExpensesTotal ?: 0.0
            }
            listTotal = monthlyRecord?.listTotal ?: 0.0
            goalsCommitted = monthlyRecord?.goalsCommittedTotal ?: 0.0
            displayShoppingItems = emptyList() // En meses pasados la lista activa no aplica
            activeGoals = emptyList()
        } else if (isFutureMonth) {
            basePeriodIncome = if (isAfterProfileEnd) {
                0.0
            } else if (monthlyRecord != null && monthlyRecord.isClosed && monthlyRecord.income <= 0.0) {
                0.0
            } else if (isVariable) {
                variableIncome ?: 0.0
            } else {
                period.calculatePeriodIncome(profile)
            }
            proportionalExtraIncome = extraIncomesTotal
            periodIncome = basePeriodIncome + proportionalExtraIncome
            displayShoppingItems = emptyList()
            listTotal = 0.0
            extraTotal = extraExpenses.sumOf { it.amount }
            activeGoals = emptyList()
            goalsCommitted = 0.0
        } else {
            // Day, Week, Fortnight, Custom
            val isBeforeProfileStart = profileStartDate > 0L && period.endTimestamp < profileStartDate
            basePeriodIncome = if (isBeforeProfileStart || isAfterProfileEnd) {
                0.0
            } else if (isVariable) {
                val mIncome = variableIncome ?: 0.0
                (mIncome / 30.0) * period.durationDays
            } else {
                period.calculatePeriodIncome(profile)
            }
            proportionalExtraIncome = (extraIncomesTotal / 30.0) * period.durationDays
            periodIncome = basePeriodIncome + proportionalExtraIncome
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
            basePeriodIncome = basePeriodIncome,
            extraIncomesTotal = extraIncomesTotal,
            proportionalExtraIncome = proportionalExtraIncome,
            listTotal = listTotal,
            extraExpensesTotal = extraTotal,
            goalsCommittedTotal = goalsCommitted,
            shoppingItems = displayShoppingItems,
            extraExpenses = extraExpenses,
            extraIncomes = extraIncomes,
            activeGoals = activeGoals,
            currency = profile?.currency ?: "MXN"
        )
    }

    private data class DialogsState(
        val showDatePicker: Boolean = false,
        val showDateRangePicker: Boolean = false,
        val editingExpense: ExtraExpense? = null,
        val isAddExpenseSheetOpen: Boolean = false,
        val isFinancialCalendarOpen: Boolean = false,
        val calendarSelectedDate: Long? = null,
        val isIncomeSheetOpen: Boolean = false,
        val isDeleteSalaryDialogOpen: Boolean = false,
        val pendingDeleteSalaryYear: Int = 0,
        val pendingDeleteSalaryMonth: Int = 0,
        val editingIncome: ExtraIncome? = null,
        val isAddIncomeSheetOpen: Boolean = false
    )

    private val dialogsStateFlow = combine(
        combine(_showDatePicker, _showDateRangePicker, _editingExpense, _isIncomeSheetOpen) { showDate, showRange, expense, isIncomeOpen ->
            listOf(showDate, showRange, expense, isIncomeOpen)
        },
        combine(_isAddExpenseSheetOpen, _isFinancialCalendarOpen, _calendarSelectedDate) { isAdd, isCal, calDate ->
            Triple(isAdd, isCal, calDate)
        },
        combine(_isDeleteSalaryDialogOpen, _pendingDeleteSalaryYear, _pendingDeleteSalaryMonth) { isDel, year, month ->
            Triple(isDel, year, month)
        },
        combine(_editingIncome, _isAddIncomeSheetOpen) { editingInc, isAddInc ->
            Pair(editingInc, isAddInc)
        }
    ) { group1, (isAdd, isCal, calDate), (isDel, year, month), (editingInc, isAddInc) ->
        DialogsState(
            showDatePicker = group1[0] as Boolean,
            showDateRangePicker = group1[1] as Boolean,
            editingExpense = group1[2] as ExtraExpense?,
            isIncomeSheetOpen = group1[3] as Boolean,
            isAddExpenseSheetOpen = isAdd,
            isFinancialCalendarOpen = isCal,
            calendarSelectedDate = calDate,
            isDeleteSalaryDialogOpen = isDel,
            pendingDeleteSalaryYear = year,
            pendingDeleteSalaryMonth = month,
            editingIncome = editingInc,
            isAddIncomeSheetOpen = isAddInc
        )
    }

    val uiState: StateFlow<FinancialDetailUiState> = combine(
        combinedDataFlow,
        dialogsStateFlow
    ) { state, dialogs ->
        state.copy(
            showDatePickerModal = dialogs.showDatePicker,
            showDateRangePickerModal = dialogs.showDateRangePicker,
            editingExpense = dialogs.editingExpense,
            isEditExpenseSheetOpen = dialogs.editingExpense != null,
            isAddExpenseSheetOpen = dialogs.isAddExpenseSheetOpen,
            isFinancialCalendarOpen = dialogs.isFinancialCalendarOpen,
            calendarSelectedDateMillis = dialogs.calendarSelectedDate,
            isIncomeSheetOpen = dialogs.isIncomeSheetOpen,
            isDeleteSalaryDialogOpen = dialogs.isDeleteSalaryDialogOpen,
            pendingDeleteSalaryYear = dialogs.pendingDeleteSalaryYear,
            pendingDeleteSalaryMonth = dialogs.pendingDeleteSalaryMonth,
            editingIncome = dialogs.editingIncome,
            isEditIncomeSheetOpen = dialogs.editingIncome != null,
            isAddIncomeSheetOpen = dialogs.isAddIncomeSheetOpen
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

    fun openAddExpenseSheet() {
        _isAddExpenseSheetOpen.value = true
    }

    fun closeAddExpenseSheet() {
        _isAddExpenseSheetOpen.value = false
    }

    fun addExtraExpense(
        label: String,
        amount: Double,
        iconKey: String = "receipt",
        expenseDate: Long = System.currentTimeMillis()
    ) {
        if (label.isBlank() || amount <= 0.0) return
        val cal = Calendar.getInstance().apply { timeInMillis = expenseDate }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        viewModelScope.launch {
            dashboardRepository.addExtraExpense(
                label = label.trim(),
                amount = amount,
                iconKey = iconKey,
                year = year,
                month = month,
                expenseDate = expenseDate
            )
            closeAddExpenseSheet()
        }
    }

    fun openFinancialCalendar() {
        _isFinancialCalendarOpen.value = true
    }

    fun closeFinancialCalendar() {
        _isFinancialCalendarOpen.value = false
    }

    fun selectCalendarDate(dateMillis: Long?) {
        _calendarSelectedDate.value = dateMillis
    }

    fun openEditExpense(expense: ExtraExpense) {
        _editingExpense.value = expense
    }

    fun closeEditExpense() {
        _editingExpense.value = null
    }

    fun updateExtraExpense(
        id: String,
        label: String,
        amount: Double,
        expenseDate: Long = System.currentTimeMillis()
    ) {
        if (label.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            dashboardRepository.updateExtraExpense(id, label, amount, expenseDate)
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

    fun openAddExtraIncomeSheet() {
        _isAddIncomeSheetOpen.value = true
    }

    fun closeAddExtraIncomeSheet() {
        _isAddIncomeSheetOpen.value = false
    }

    fun openEditExtraIncome(income: ExtraIncome) {
        _editingIncome.value = income
    }

    fun closeEditExtraIncome() {
        _editingIncome.value = null
    }

    fun addExtraIncome(
        label: String,
        amount: Double,
        iconKey: String = "payments",
        incomeDate: Long = System.currentTimeMillis()
    ) {
        if (label.isBlank() || amount <= 0.0) return
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedPeriod.value.startTimestamp }
        val year = if (_selectedPeriod.value is FinancialPeriod.Month) (_selectedPeriod.value as FinancialPeriod.Month).year else cal.get(Calendar.YEAR)
        val month = if (_selectedPeriod.value is FinancialPeriod.Month) (_selectedPeriod.value as FinancialPeriod.Month).month else (cal.get(Calendar.MONTH) + 1)

        viewModelScope.launch {
            dashboardRepository.addExtraIncome(
                label = label,
                amount = amount,
                iconKey = iconKey,
                year = year,
                month = month,
                incomeDate = incomeDate
            )
            closeAddExtraIncomeSheet()
        }
    }

    fun updateExtraIncome(
        id: String,
        label: String,
        amount: Double,
        incomeDate: Long = System.currentTimeMillis()
    ) {
        if (label.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            dashboardRepository.updateExtraIncome(id, label, amount, incomeDate)
            closeEditExtraIncome()
        }
    }

    fun deleteExtraIncome(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteExtraIncome(id)
            if (_editingIncome.value?.id == id) {
                closeEditExtraIncome()
            }
        }
    }

    fun openIncomeSheet() {
        _isIncomeSheetOpen.value = true
    }

    fun closeIncomeSheet() {
        _isIncomeSheetOpen.value = false
    }

    fun saveIncome(
        income: Double,
        frequency: com.farbalapps.rinde.domain.model.IncomeFrequency,
        currency: String = "MXN",
        customStartDate: Long? = null,
        customEndDate: Long? = null,
        isVariable: Boolean = false,
        paymentDate: Long? = null
    ) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { timeInMillis = _selectedPeriod.value.startTimestamp }
            val currentPeriodYear = if (_selectedPeriod.value is FinancialPeriod.Month) (_selectedPeriod.value as FinancialPeriod.Month).year else cal.get(Calendar.YEAR)
            val currentPeriodMonth = if (_selectedPeriod.value is FinancialPeriod.Month) (_selectedPeriod.value as FinancialPeriod.Month).month else (cal.get(Calendar.MONTH) + 1)

            val previousProfile = dashboardRepository.getFinancialProfile().firstOrNull()
            if (previousProfile != null && previousProfile.income > 0.0) {
                val prevMonth = if (currentPeriodMonth == 1) 12 else currentPeriodMonth - 1
                val prevYear = if (currentPeriodMonth == 1) currentPeriodYear - 1 else currentPeriodYear
                val existingRecord = dashboardRepository.getMonthlyRecord(prevYear, prevMonth).firstOrNull()
                if (existingRecord == null || existingRecord.income <= 0.0) {
                    val prevExpenses = dashboardRepository.getExtraExpenses(prevYear, prevMonth).firstOrNull() ?: emptyList()
                    val extraTotal = prevExpenses.sumOf { it.amount }
                    val prevAvailable = previousProfile.income - extraTotal
                    val healthStatus = when {
                        previousProfile.income <= 0.0 -> "GOOD"
                        prevAvailable < 0.0 -> "CRITICAL"
                        (extraTotal / previousProfile.income) > 0.85 -> "WARNING"
                        else -> "GOOD"
                    }
                    val record = MonthlyFinancialRecord(
                        id = "${previousProfile.id}_${prevYear}_${prevMonth}",
                        userId = previousProfile.id,
                        year = prevYear,
                        month = prevMonth,
                        income = previousProfile.income,
                        extraExpensesTotal = extraTotal,
                        listTotal = 0.0,
                        goalsCommittedTotal = 0.0,
                        availableAmount = prevAvailable,
                        healthStatus = healthStatus,
                        isClosed = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    dashboardRepository.saveMonthlyRecord(record)
                }
            }

            dashboardRepository.saveFinancialProfile(
                income = income,
                frequency = frequency,
                currency = currency,
                customStartDate = customStartDate,
                customEndDate = customEndDate,
                isVariableIncome = isVariable
            )

            if (isVariable) {
                dashboardRepository.saveMonthlyVariableIncome(
                    year = currentPeriodYear,
                    month = currentPeriodMonth,
                    amount = income,
                    paymentDate = paymentDate ?: System.currentTimeMillis()
                )
            }
            closeIncomeSheet()
        }
    }

    fun openDeleteSalaryDialog(year: Int, month: Int) {
        _pendingDeleteSalaryYear.value = year
        _pendingDeleteSalaryMonth.value = month
        _isDeleteSalaryDialogOpen.value = true
    }

    fun closeDeleteSalaryDialog() {
        _isDeleteSalaryDialogOpen.value = false
        _pendingDeleteSalaryYear.value = 0
        _pendingDeleteSalaryMonth.value = 0
    }

    fun deleteSalarySingleMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val profile = dashboardRepository.getFinancialProfile().firstOrNull()
            if (profile != null) {
                val expenses = dashboardRepository.getExtraExpenses(year, month).firstOrNull() ?: emptyList()
                val extraTotal = expenses.sumOf { it.amount }
                val record = MonthlyFinancialRecord(
                    id = "${profile.id}_${year}_${month}",
                    userId = profile.id,
                    year = year,
                    month = month,
                    income = 0.0,
                    extraExpensesTotal = extraTotal,
                    listTotal = 0.0,
                    goalsCommittedTotal = 0.0,
                    availableAmount = -extraTotal,
                    healthStatus = "CRITICAL",
                    isClosed = true,
                    updatedAt = System.currentTimeMillis()
                )
                dashboardRepository.saveMonthlyRecord(record)
                if (profile.isVariableIncome) {
                    dashboardRepository.saveMonthlyVariableIncome(year, month, 0.0)
                }
            }
            closeDeleteSalaryDialog()
        }
    }

    fun deleteSalaryFutureMonths(year: Int, month: Int) {
        viewModelScope.launch {
            val profile = dashboardRepository.getFinancialProfile().firstOrNull()
            if (profile != null) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val prevMonthEnd = cal.timeInMillis
                val profileStartDate = profile.customStartDate ?: profile.updatedAt
                if (profileStartDate > 0L && prevMonthEnd < profileStartDate) {
                    dashboardRepository.saveFinancialProfile(
                        income = 0.0,
                        frequency = profile.incomeFrequency,
                        currency = profile.currency,
                        customStartDate = profile.customStartDate,
                        customEndDate = null,
                        isVariableIncome = profile.isVariableIncome
                    )
                } else {
                    dashboardRepository.saveFinancialProfile(
                        income = profile.income,
                        frequency = profile.incomeFrequency,
                        currency = profile.currency,
                        customStartDate = profile.customStartDate,
                        customEndDate = prevMonthEnd,
                        isVariableIncome = profile.isVariableIncome
                    )
                }
            }
            closeDeleteSalaryDialog()
        }
    }

    fun deleteSalaryAll() {
        viewModelScope.launch {
            val profile = dashboardRepository.getFinancialProfile().firstOrNull()
            if (profile != null) {
                dashboardRepository.saveFinancialProfile(
                    income = 0.0,
                    frequency = profile.incomeFrequency,
                    currency = profile.currency,
                    customStartDate = profile.customStartDate,
                    customEndDate = null,
                    isVariableIncome = profile.isVariableIncome
                )
            }
            closeDeleteSalaryDialog()
        }
    }
}
