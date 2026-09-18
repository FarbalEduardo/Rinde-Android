package com.farbalapps.rinde.ui.screen.home.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.IncomeFrequency
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.repository.SavedListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val listRepository: ListRepository,
    private val goalsRepository: GoalsRepository,
    private val savedListRepository: SavedListRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    val currentMonth: Int = calendar.get(Calendar.MONTH) + 1 // 1-12
    val currentYear: Int = calendar.get(Calendar.YEAR)

    val currentMonthName: String = run {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val name = formatter.format(calendar.time)
        name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private val _isIncomeSheetOpen = MutableStateFlow(false)
    private val _isExpenseSheetOpen = MutableStateFlow(false)
    private val _editingExpense = MutableStateFlow<ExtraExpense?>(null)

    init {
        viewModelScope.launch {
            dashboardRepository.syncFromFirebase()
            dashboardRepository.checkAndPerformMonthlyRollover()
            goalsRepository.syncGoals()
            savedListRepository.syncSavedLists()
        }
    }

    private val repositoryDataFlow = combine(
        dashboardRepository.getFinancialProfile(),
        listRepository.getItems(),
        goalsRepository.getGoals(),
        dashboardRepository.getExtraExpenses(currentYear, currentMonth)
    ) { profile, shoppingItems, goals, extraExpenses ->

        val monthlyIncome = profile?.monthlyEquivalent ?: 0.0

        // Mi Lista: únicamente artículos marcados como comprados (isCompleted = true)
        val boughtItems = shoppingItems.filter { it.isCompleted }
        val listTotal = boughtItems.sumOf { item ->
            (item.price ?: 0.0) * item.quantity
        }

        // Gastos Extra del Hogar (Luz, Agua, Renta, etc.)
        val extraTotal = extraExpenses.sumOf { it.amount }

        // Metas activas: no archivadas (incluye cumplidas reactivadas)
        val activeGoals = goals.filter { !it.isArchived }

        // Total comprometido/ahorrado en metas este mes
        val monthlyGoalsSavings = activeGoals.sumOf { goal ->
            val delta = goal.currentAmount - goal.monthlySnapshotAmount
            if (delta > 0.0) delta else 0.0
        }

        DashboardUiState(
            isLoading = false,
            currentMonthName = currentMonthName,
            currentMonth = currentMonth,
            currentYear = currentYear,
            profile = profile,
            monthlyIncome = monthlyIncome,
            listTotal = listTotal,
            extraExpensesTotal = extraTotal,
            goalsCommittedTotal = monthlyGoalsSavings,
            extraExpenses = extraExpenses,
            activeGoals = activeGoals,
            currency = profile?.currency ?: "MXN"
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repositoryDataFlow,
        _isIncomeSheetOpen,
        _isExpenseSheetOpen,
        _editingExpense
    ) { state, isIncomeOpen, isExpenseOpen, editingExpense ->
        state.copy(
            isIncomeSheetOpen = isIncomeOpen,
            isExpenseSheetOpen = isExpenseOpen,
            editingExpense = editingExpense,
            isEditExpenseSheetOpen = editingExpense != null
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
        customEndDate: Long? = null
    ) {
        viewModelScope.launch {
            dashboardRepository.saveFinancialProfile(
                income = income,
                frequency = frequency,
                currency = currency,
                customStartDate = customStartDate,
                customEndDate = customEndDate
            )
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

    fun addExtraExpense(label: String, amount: Double, iconKey: String = "receipt") {
        if (label.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            dashboardRepository.addExtraExpense(
                label = label.trim(),
                amount = amount,
                iconKey = iconKey,
                year = currentYear,
                month = currentMonth
            )
            closeExpenseSheet()
        }
    }

    fun updateExtraExpense(id: String, label: String, amount: Double) {
        if (label.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            dashboardRepository.updateExtraExpense(id, label, amount)
            closeEditExpenseSheet()
        }
    }

    fun deleteExtraExpense(id: String) {
        viewModelScope.launch {
            dashboardRepository.deleteExtraExpense(id)
            if (_editingExpense.value?.id == id) {
                closeEditExpenseSheet()
            }
        }
    }
}
