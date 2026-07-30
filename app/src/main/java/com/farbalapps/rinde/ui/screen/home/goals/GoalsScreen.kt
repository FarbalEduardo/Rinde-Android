package com.farbalapps.rinde.ui.screen.home.goals

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    // Control de BottomSheets y diálogos
    var showCreateBottomSheetInternal by remember { mutableStateOf(false) }
    var activeDepositGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

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
                    Toast.makeText(context, "Límite de metas alcanzado (máximo 3).", Toast.LENGTH_SHORT).show()
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

    // Alerta de confirmación de depósito excedente
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

    // Alerta de eliminación de meta
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mis Objetivos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones de Metas"
                            )
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nueva Meta") },
                                onClick = {
                                    showOptionsMenu = false
                                    showCreateBottomSheetInternal = true
                                }
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
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
                    val allGoals = remember(state) {
                        listOfNotNull(state.featuredGoal) + state.secondaryGoals
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header de Resumen Total (Estilo lalo 2)
                        item {
                            GoalsSummaryHeader(summary = state.summary)
                        }

                        // Sección: Objetivos Activos
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Objetivos activos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${allGoals.size} de 3 metas",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Lista de Metas (Hasta 3 metas caben limpiamente)
                        items(allGoals, key = { it.id }) { goal ->
                            FeaturedGoalCard(
                                goal = goal,
                                onClick = { activeDepositGoal = goal },
                                onLongClick = { goalToDelete = goal }
                            )
                        }

                        // Tarjeta comodín punteada si aún quedan cupos para metas (menos de 3)
                        if (allGoals.size < 3) {
                            item {
                                DashedAddGoalCard(
                                    currentGoalCount = allGoals.size,
                                    onClick = { showCreateBottomSheetInternal = true }
                                )
                            }
                        }

                        // Recomendación aleatoria de IA Chef (1 de 15 frases)
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            ChefRandomRecommendationCard()
                        }
                    }
                }
            }
        }
    }

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
}
