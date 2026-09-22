package com.farbalapps.rinde.ui.screen.home.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.ExtraIncome
import com.farbalapps.rinde.domain.model.IncomeFrequency
import com.farbalapps.rinde.domain.model.MonthlyFinancialRecord
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.usecase.dashboard.GetDashboardSummaryUseCase
import com.farbalapps.rinde.domain.usecase.dashboard.ManageExtraExpenseUseCase
import com.farbalapps.rinde.domain.usecase.dashboard.ManageExtraIncomeUseCase
import com.farbalapps.rinde.domain.usecase.dashboard.SyncDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val manageExtraExpenseUseCase: ManageExtraExpenseUseCase,
    private val manageExtraIncomeUseCase: ManageExtraIncomeUseCase,
    private val syncDashboardDataUseCase: SyncDashboardDataUseCase,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    val currentMonth: Int = calendar.get(Calendar.MONTH) + 1 // 1-12
    val currentYear: Int = calendar.get(Calendar.YEAR)

    val currentMonthName: String = run {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val name = formatter.format(calendar.time)
        name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private val _isRefreshing = MutableStateFlow(false)
    private val _isFinancialCalendarOpen = MutableStateFlow(false)
    private val _isIncomeSheetOpen = MutableStateFlow(false)
    private val _isExpenseSheetOpen = MutableStateFlow(false)
    private val _editingExpense = MutableStateFlow<ExtraExpense?>(null)
    private val _isExtraIncomeSheetOpen = MutableStateFlow(false)
    private val _editingExtraIncome = MutableStateFlow<ExtraIncome?>(null)
    private val _pendingDeleteExpense = MutableStateFlow<ExtraExpense?>(null)
    private val _pendingDeleteIncome = MutableStateFlow<ExtraIncome?>(null)

    init {
        viewModelScope.launch {
            syncDashboardDataUseCase()
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncDashboardDataUseCase()
            _isRefreshing.value = false
        }
    }

    fun openFinancialCalendar() {
        _isFinancialCalendarOpen.value = true
    }

    fun closeFinancialCalendar() {
        _isFinancialCalendarOpen.value = false
    }

    fun requestDeleteExpense(expense: ExtraExpense) {
        _pendingDeleteExpense.value = expense
    }

    fun confirmDeleteExpense() {
        val expense = _pendingDeleteExpense.value ?: return
        viewModelScope.launch {
            manageExtraExpenseUseCase.deleteExpense(expense.id)
            _pendingDeleteExpense.value = null
            if (_editingExpense.value?.id == expense.id) {
                closeEditExpenseSheet()
            }
        }
    }

    fun cancelDeleteExpense() {
        _pendingDeleteExpense.value = null
    }

    fun requestDeleteIncome(income: ExtraIncome) {
        _pendingDeleteIncome.value = income
    }

    fun confirmDeleteIncome() {
        val income = _pendingDeleteIncome.value ?: return
        viewModelScope.launch {
            manageExtraIncomeUseCase.deleteIncome(income.id)
            _pendingDeleteIncome.value = null
            if (_editingExtraIncome.value?.id == income.id) {
                closeEditExtraIncomeSheet()
            }
        }
    }

    fun cancelDeleteIncome() {
        _pendingDeleteIncome.value = null
    }

    private data class DialogAndSheetStates(
        val isIncomeOpen: Boolean,
        val isExpenseOpen: Boolean,
        val editingExpense: ExtraExpense?,
        val isExtraIncomeOpen: Boolean,
        val editingExtraIncome: ExtraIncome?,
        val isCalendarOpen: Boolean,
        val isRefreshing: Boolean,
        val pendingDeleteExpense: ExtraExpense?,
        val pendingDeleteIncome: ExtraIncome?
    )

    private val dialogsStateFlow = combine(
        combine(_isIncomeSheetOpen, _isExpenseSheetOpen, _editingExpense) { a, b, c -> Triple(a, b, c) },
        combine(_isExtraIncomeSheetOpen, _editingExtraIncome, _isFinancialCalendarOpen) { a, b, c -> Triple(a, b, c) },
        combine(_isRefreshing, _pendingDeleteExpense, _pendingDeleteIncome) { a, b, c -> Triple(a, b, c) }
    ) { (isIncomeOpen, isExpenseOpen, editingExpense),
        (isExtraIncomeOpen, editingExtraIncome, isCalendarOpen),
        (isRefreshing, pendingDeleteExpense, pendingDeleteIncome) ->
        DialogAndSheetStates(
            isIncomeOpen = isIncomeOpen,
            isExpenseOpen = isExpenseOpen,
            editingExpense = editingExpense,
            isExtraIncomeOpen = isExtraIncomeOpen,
            editingExtraIncome = editingExtraIncome,
            isCalendarOpen = isCalendarOpen,
            isRefreshing = isRefreshing,
            pendingDeleteExpense = pendingDeleteExpense,
            pendingDeleteIncome = pendingDeleteIncome
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        getDashboardSummaryUseCase(currentYear, currentMonth),
        dialogsStateFlow
    ) { summary, dialogs ->
        DashboardUiState(
            isLoading = false,
            isRefreshing = dialogs.isRefreshing,
            currentMonthName = currentMonthName,
            currentMonth = currentMonth,
            currentYear = currentYear,
            profile = summary.profile,
            isVariableIncome = summary.isVariableIncome,
            baseMonthlyIncome = summary.baseMonthlyIncome,
            monthlyIncome = summary.monthlyIncome,
            listTotal = summary.listTotal,
            extraExpensesTotal = summary.extraExpensesTotal,
            extraIncomesTotal = summary.extraIncomesTotal,
            goalsCommittedTotal = summary.goalsCommittedTotal,
            extraExpenses = summary.extraExpenses,
            extraIncomes = summary.extraIncomes,
            activeGoals = summary.activeGoals,
            currency = summary.currency,
            isIncomeSheetOpen = dialogs.isIncomeOpen,
            isExpenseSheetOpen = dialogs.isExpenseOpen,
            editingExpense = dialogs.editingExpense,
            isEditExpenseSheetOpen = dialogs.editingExpense != null,
            isExtraIncomeSheetOpen = dialogs.isExtraIncomeOpen,
            editingExtraIncome = dialogs.editingExtraIncome,
            isEditExtraIncomeSheetOpen = dialogs.editingExtraIncome != null,
            isFinancialCalendarOpen = dialogs.isCalendarOpen,
            pendingDeleteExpense = dialogs.pendingDeleteExpense,
            pendingDeleteIncome = dialogs.pendingDeleteIncome
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            currentMonthName = currentMonthName,
            currentMonth = currentMonth,
            currentYear = currentYear
        )
    )

    fun openIncomeSheet() {
        _isIncomeSheetOpen.value = true
    }

    fun closeIncomeSheet() {
        _isIncomeSheetOpen.value = false
    }

    fun saveIncome(
        income: Double,
        frequency: IncomeFrequency,
        currency: String = "MXN",
        customStartDate: Long? = null,
        customEndDate: Long? = null,
        isVariable: Boolean = false,
        paymentDate: Long? = null
    ) {
        viewModelScope.launch {
            val previousProfile = dashboardRepository.getFinancialProfile().firstOrNull()
            val effectiveCurrency = previousProfile?.currency ?: currency
            if (previousProfile != null && previousProfile.income > 0.0) {
                val prevMonth = if (currentMonth == 1) 12 else currentMonth - 1
                val prevYear = if (currentMonth == 1) currentYear - 1 else currentYear
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
                currency = effectiveCurrency,
                customStartDate = customStartDate,
                customEndDate = customEndDate,
                isVariableIncome = isVariable
            )
            if (isVariable) {
                dashboardRepository.saveMonthlyVariableIncome(
                    year = currentYear,
                    month = currentMonth,
                    amount = income,
                    paymentDate = paymentDate ?: System.currentTimeMillis()
                )
            }
            closeIncomeSheet()
        }
    }

    fun openExpenseSheet() {
        _isExpenseSheetOpen.value = true
    }

    fun closeExpenseSheet() {
        _isExpenseSheetOpen.value = false
    }

    fun openEditExpenseSheet(expense: ExtraExpense) {
        _editingExpense.value = expense
    }

    fun closeEditExpenseSheet() {
        _editingExpense.value = null
    }

    fun addExtraExpense(
        label: String,
        amount: Double,
        iconKey: String = "receipt",
        expenseDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            manageExtraExpenseUseCase.addExpense(
                label = label,
                amount = amount,
                iconKey = iconKey,
                year = currentYear,
                month = currentMonth,
                expenseDate = expenseDate
            )
            closeExpenseSheet()
        }
    }

    fun updateExtraExpense(
        id: String,
        label: String,
        amount: Double,
        expenseDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            manageExtraExpenseUseCase.updateExpense(id, label, amount, expenseDate)
            closeEditExpenseSheet()
        }
    }

    fun deleteExtraExpense(id: String) {
        viewModelScope.launch {
            manageExtraExpenseUseCase.deleteExpense(id)
            if (_editingExpense.value?.id == id) {
                closeEditExpenseSheet()
            }
        }
    }

    fun openExtraIncomeSheet() {
        _isExtraIncomeSheetOpen.value = true
    }

    fun closeExtraIncomeSheet() {
        _isExtraIncomeSheetOpen.value = false
    }

    fun openEditExtraIncomeSheet(income: ExtraIncome) {
        _editingExtraIncome.value = income
    }

    fun closeEditExtraIncomeSheet() {
        _editingExtraIncome.value = null
    }

    fun addExtraIncome(
        label: String,
        amount: Double,
        iconKey: String = "cash",
        incomeDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            manageExtraIncomeUseCase.addIncome(
                label = label,
                amount = amount,
                iconKey = iconKey,
                year = currentYear,
                month = currentMonth,
                incomeDate = incomeDate
            )
            closeExtraIncomeSheet()
        }
    }

    fun updateExtraIncome(
        id: String,
        label: String,
        amount: Double,
        incomeDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            manageExtraIncomeUseCase.updateIncome(id, label, amount, incomeDate)
            closeEditExtraIncomeSheet()
        }
    }

    fun deleteExtraIncome(id: String) {
        viewModelScope.launch {
            manageExtraIncomeUseCase.deleteIncome(id)
            if (_editingExtraIncome.value?.id == id) {
                closeEditExtraIncomeSheet()
            }
        }
    }

    fun deleteSalarySingleMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val uid = dashboardRepository.getFinancialProfile().firstOrNull()?.id ?: "user"
            val record = MonthlyFinancialRecord(
                id = "${uid}_${year}_${month}",
                userId = uid,
                year = year,
                month = month,
                income = 0.0,
                extraExpensesTotal = 0.0,
                listTotal = 0.0,
                goalsCommittedTotal = 0.0,
                availableAmount = 0.0,
                healthStatus = "GOOD",
                isClosed = true,
                updatedAt = System.currentTimeMillis()
            )
            dashboardRepository.saveMonthlyRecord(record)
        }
    }

    fun deleteSalaryFutureMonths(year: Int, month: Int) {
        viewModelScope.launch {
            val profile = dashboardRepository.getFinancialProfile().firstOrNull() ?: return@launch
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, 1, 0, 0, 0)
                add(Calendar.DAY_OF_MONTH, -1)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            dashboardRepository.saveFinancialProfile(
                income = profile.income,
                frequency = profile.incomeFrequency,
                currency = profile.currency,
                customStartDate = profile.customStartDate,
                customEndDate = cal.timeInMillis,
                isVariableIncome = profile.isVariableIncome
            )
        }
    }

    fun deleteSalaryAll() {
        viewModelScope.launch {
            val profile = dashboardRepository.getFinancialProfile().firstOrNull()
            dashboardRepository.saveFinancialProfile(
                income = 0.0,
                frequency = profile?.incomeFrequency ?: IncomeFrequency.MONTHLY,
                currency = profile?.currency ?: "MXN",
                customStartDate = null,
                customEndDate = null,
                isVariableIncome = false
            )
        }
    }
}
