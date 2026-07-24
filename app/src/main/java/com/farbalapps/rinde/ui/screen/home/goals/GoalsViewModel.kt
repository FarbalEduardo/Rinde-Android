package com.farbalapps.rinde.ui.screen.home.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.usecase.goals.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val getGoalsSummaryUseCase: GetGoalsSummaryUseCase,
    private val createGoalUseCase: CreateGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val depositToGoalUseCase: DepositToGoalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GoalsUiState>(GoalsUiState.Loading)
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GoalsEvent>()
    val events: SharedFlow<GoalsEvent> = _events.asSharedFlow()

    init {
        loadGoalsData()
    }

    private fun loadGoalsData() {
        viewModelScope.launch {
            combine(
                getGoalsUseCase(),
                getGoalsSummaryUseCase()
            ) { goals, summary ->
                if (goals.isEmpty()) {
                    GoalsUiState.Empty
                } else {
                    // E1.2 - E1.4: Lógica del layout adaptativo
                    // La primera de la lista ordenada por cantidad ahorrada es la destacada
                    val featured = goals.firstOrNull()
                    val secondary = if (goals.size > 1) goals.drop(1) else emptyList()
                    
                    GoalsUiState.Content(
                        featuredGoal = featured,
                        secondaryGoals = secondary,
                        summary = summary,
                        canAddMore = goals.size < CreateGoalUseCase.FREE_TIER_LIMIT
                    )
                }
            }.catch { e ->
                _uiState.value = GoalsUiState.Error(e.localizedMessage ?: "Error desconocido")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun createGoal(title: String, targetAmount: Double, iconKey: String, colorKey: String) {
        viewModelScope.launch {
            val newGoal = SavingsGoal(
                id = UUID.randomUUID().toString(),
                userId = "", // Se resuelve en el repositorio a nivel del ID de Firebase actual
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                iconKey = iconKey,
                colorKey = colorKey,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                monthlySnapshotAmount = 0.0
            )
            createGoalUseCase(newGoal).onSuccess {
                _events.emit(GoalsEvent.Success("¡Meta creada exitosamente!"))
            }.onFailure { e ->
                _events.emit(GoalsEvent.ValidationError(e.message ?: "Datos inválidos"))
            }
        }
    }

    fun deposit(goalId: String, amount: Double, note: String, force: Boolean = false) {
        viewModelScope.launch {
            depositToGoalUseCase(goalId, amount, note, force).onSuccess { result ->
                when (result) {
                    is DepositResult.RequiresConfirmation -> {
                        _events.emit(GoalsEvent.DepositExceedsTarget(goalId, amount, result.excess, note))
                    }
                    is DepositResult.Success -> {
                        if (result.completed) {
                            val goal = (uiState.value as? GoalsUiState.Content)?.featuredGoal ?: return@launch
                            _events.emit(GoalsEvent.GoalCompleted(goal.title))
                        } else {
                            _events.emit(GoalsEvent.Success("¡Depósito registrado con éxito!"))
                        }
                    }
                }
            }.onFailure { e ->
                _events.emit(GoalsEvent.ValidationError(e.message ?: "Error al depositar"))
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            deleteGoalUseCase(goalId).onSuccess {
                _events.emit(GoalsEvent.Success("Meta eliminada"))
            }.onFailure { e ->
                _events.emit(GoalsEvent.ValidationError(e.message ?: "Error al eliminar"))
            }
        }
    }
}
