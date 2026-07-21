package com.farbalapps.rinde.ui.screen.home.goals

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.ui.screen.home.goals.components.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    showCreateBottomSheetExternal: Boolean = false,
    onDismissCreateBottomSheet: () -> Unit = {},
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Control de BottomSheets
    var showCreateBottomSheetInternal by remember { mutableStateOf(false) }
    var activeDepositGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }

    val showCreateBottomSheet = showCreateBottomSheetExternal || showCreateBottomSheetInternal

    // Diálogos de advertencia y confirmación
    var showExcessConfirmation by remember { mutableStateOf<GoalsEvent.DepositExceedsTarget?>(null) }

    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GoalsEvent.Success -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    showCreateBottomSheetInternal = false
                    activeDepositGoal = null
                }
                is GoalsEvent.ValidationError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is GoalsEvent.GoalLimitReached -> {
                    Toast.makeText(context, "Límite de metas alcanzado.", Toast.LENGTH_SHORT).show()
                }
                is GoalsEvent.GoalCompleted -> {
                    Toast.makeText(context, "🎉 ¡Felicidades! Completaste la meta: ${event.title}", Toast.LENGTH_LONG).show()
                    activeDepositGoal = null
                }
                is GoalsEvent.DepositExceedsTarget -> {
                    showExcessConfirmation = event
                }
            }
        }
    }

    // Alerta de confirmación de depósito excedente (E3.4 Opción B)
    showExcessConfirmation?.let { event ->
        AlertDialog(
            onDismissRequest = { showExcessConfirmation = null },
            title = { Text("Confirmar Depósito Excedente") },
            text = { Text("Este depósito supera tu meta por $${String.format("%.2f", event.excess)}. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deposit(event.goalId, event.amount, event.note, force = true)
                        showExcessConfirmation = null
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExcessConfirmation = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Alerta de eliminación (E5.1)
    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Eliminar Meta") },
            text = { Text("¿Estás seguro de que deseas eliminar la meta \"${goal.title}\"? El dinero ahorrado ($${String.format("%.2f", goal.currentAmount)}) dejará de sumarse en el total.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal(goal.id)
                        goalToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
            when (val state = uiState) {
                is GoalsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GoalsUiState.Empty -> {
                    EmptyGoalsContent(
                        onCreateFirstGoalClick = { showCreateBottomSheetInternal = true }
                    )
                }
                is GoalsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is GoalsUiState.Content -> {
                    // Diseño flexible de rejilla (LazyVerticalGrid) para dar soporte al responsive y al grid dinámico
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header de resumen (Ocupa ambas columnas)
                        item(span = { GridItemSpan(2) }) {
                            GoalsSummaryHeader(summary = state.summary)
                        }

                        // Tarjeta Destacada (Featured Card) (Ocupa ambas columnas) (E1.2, E1.3, E1.4)
                        state.featuredGoal?.let { goal ->
                            item(span = { GridItemSpan(2) }) {
                                FeaturedGoalCard(
                                    goal = goal,
                                    onClick = { activeDepositGoal = goal },
                                    onLongClick = { goalToDelete = goal }
                                )
                            }
                        }

                        // Tarjetas Secundarias (Ocupan una columna cada una para formar un grid de 2 columnas)
                        items(state.secondaryGoals) { goal ->
                            SmallGoalCard(
                                goal = goal,
                                onClick = { activeDepositGoal = goal },
                                onLongClick = { goalToDelete = goal }
                            )
                        }

                        // AI Suggestion & Fondo de Emergencia (Decorativos y mockeados del template original)
                        item(span = { GridItemSpan(2) }) {
                            ChefSuggestionCard()
                        }
                        item(span = { GridItemSpan(2) }) {
                            EmergencyFundCard()
                        }
                        item(span = { GridItemSpan(2) }) {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
    } // cierre Surface

    // Modal para Crear Metas
    if (showCreateBottomSheet) {
        CreateGoalBottomSheet(
            onDismissRequest = {
                showCreateBottomSheetInternal = false
                onDismissCreateBottomSheet()
            },
            onConfirm = { title, target, icon, color ->
                viewModel.createGoal(title, target, icon, color)
            }
        )
    }

    // Modal para Registrar Depósito
    activeDepositGoal?.let { goal ->
        DepositBottomSheet(
            goalTitle = goal.title,
            onDismissRequest = { activeDepositGoal = null },
            onConfirm = { amount, note ->
                viewModel.deposit(goal.id, amount, note)
            }
        )
    }
} // cierre GoalsScreen
