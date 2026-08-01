package com.farbalapps.rinde.ui.screen.home.goals.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.farbalapps.rinde.domain.model.GoalTransaction
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.usecase.goals.DepositResult
import com.farbalapps.rinde.domain.usecase.goals.DepositToGoalUseCase
import com.farbalapps.rinde.domain.usecase.goals.GetGoalByIdUseCase
import com.farbalapps.rinde.domain.usecase.goals.GetGoalTransactionsUseCase
import com.farbalapps.rinde.domain.usecase.goals.CalculateGoalSuggestionUseCase
import com.farbalapps.rinde.ui.navigation.HomeRoute
import com.farbalapps.rinde.ui.screen.home.goals.GoalsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGoalByIdUseCase: GetGoalByIdUseCase,
    private val getGoalTransactionsUseCase: GetGoalTransactionsUseCase,
    private val calculateGoalSuggestionUseCase: CalculateGoalSuggestionUseCase,
    private val depositToGoalUseCase: DepositToGoalUseCase
) : ViewModel() {

    val goalId: String = try {
        savedStateHandle.toRoute<HomeRoute.GoalDetail>().goalId
    } catch (_: Exception) {
        savedStateHandle.get<String>("goalId") ?: ""
    }

    private val _uiState = MutableStateFlow<GoalDetailUiState>(GoalDetailUiState.Loading)
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GoalsEvent>()
    val events: SharedFlow<GoalsEvent> = _events.asSharedFlow()

    init {
        if (goalId.isNotEmpty()) {
            loadGoalDetail()
        } else {
            _uiState.value = GoalDetailUiState.Error("Identificador de meta no encontrado")
        }
    }

    private fun loadGoalDetail() {
        viewModelScope.launch {
            combine(
                getGoalByIdUseCase(goalId),
                getGoalTransactionsUseCase(goalId)
            ) { goal, transactions ->
                if (goal == null) {
                    GoalDetailUiState.Error("Meta no encontrada")
                } else {
                    buildContentState(goal, transactions)
                }
            }.catch { e ->
                _uiState.value = GoalDetailUiState.Error(e.localizedMessage ?: "Error al cargar la meta")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildContentState(goal: SavingsGoal, transactions: List<GoalTransaction>): GoalDetailUiState.Content {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val creationDateStr = dateFormat.format(Date(goal.createdAt))
        
        val limitDateStr = goal.targetDate?.let { dateFormat.format(Date(it)) } ?: "Sin límite"

        // Estimación de aportación mensual usando el caso de uso
        val monthlyAmount = calculateGoalSuggestionUseCase(goal)
        val monthlyContributionStr = if (monthlyAmount > 0) {
            String.format(Locale.getDefault(), "$%.2f / mes", monthlyAmount)
        } else {
            "Meta al día"
        }

        // Última aportación registrada
        val lastTransaction = transactions.maxByOrNull { it.timestamp }
        val lastContributionStr = if (lastTransaction != null) {
            dateFormat.format(Date(lastTransaction.timestamp))
        } else {
            "No registrado aún"
        }

        val categoryName = mapIconToCategory(goal.iconKey)

        return GoalDetailUiState.Content(
            goal = goal,
            transactions = transactions,
            categoryName = categoryName,
            formattedCreationDate = creationDateStr,
            formattedLimitDate = limitDateStr,
            formattedMonthlyContribution = monthlyContributionStr,
            formattedLastContribution = lastContributionStr
        )
    }

    fun deposit(amount: Double, note: String, force: Boolean = false) {
        viewModelScope.launch {
            depositToGoalUseCase(goalId, amount, note, force).onSuccess { result ->
                when (result) {
                    is DepositResult.RequiresConfirmation -> {
                        _events.emit(GoalsEvent.DepositExceedsTarget(goalId, amount, result.excess, note))
                    }
                    is DepositResult.Success -> {
                        val currentGoal = (_uiState.value as? GoalDetailUiState.Content)?.goal
                        if (result.completed && currentGoal != null) {
                            _events.emit(GoalsEvent.GoalCompleted(currentGoal.title))
                        } else {
                            _events.emit(GoalsEvent.Success("¡Abono registrado con éxito!"))
                        }
                    }
                }
            }.onFailure { e ->
                _events.emit(GoalsEvent.ValidationError(e.message ?: "Error al registrar el abono"))
            }
        }
    }

    // Ya no se requiere deleteGoal aquí ya que está en GoalsViewModel o podríamos agregar DeleteGoalUseCase si lo necesitamos
    // Pero lo dejaremos por si acaso se llama desde la vista
    fun deleteGoal(onDeleted: () -> Unit) {
        // TODO: Mover la lógica de borrar a un UseCase si la pantalla de detalle tiene un botón de borrar
    }

    private fun mapIconToCategory(iconKey: String): String {
        return when (iconKey.lowercase()) {
            "flight" -> "Viaje"
            "home" -> "Hogar"
            "laptop" -> "Tecnología"
            "car" -> "Vehículo"
            "shopping" -> "Compras"
            "education" -> "Educación"
            "savings" -> "Ahorro"
            else -> "General"
        }
    }
}
