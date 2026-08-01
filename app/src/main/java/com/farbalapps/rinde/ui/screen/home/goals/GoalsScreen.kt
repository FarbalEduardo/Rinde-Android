package com.farbalapps.rinde.ui.screen.home.goals

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    onGoalClick: (String) -> Unit = {},
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GoalsEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is GoalsEvent.ValidationError -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is GoalsEvent.GoalLimitReached -> Toast.makeText(context, "Límite de metas alcanzado (máximo 2).", Toast.LENGTH_SHORT).show()
                is GoalsEvent.GoalCompleted -> Toast.makeText(context, "🎉 ¡Felicidades! Completaste la meta: ${event.title}", Toast.LENGTH_LONG).show()
                is GoalsEvent.DepositExceedsTarget -> { /* Handled in state internally if needed or passed down, but for simplicity let's assume we handle it in content */ }
            }
        }
    }

    GoalsScreenContent(
        uiState = uiState,
        innerPadding = innerPadding,
        showCreateBottomSheetExternal = showCreateBottomSheetExternal,
        onDismissCreateBottomSheet = onDismissCreateBottomSheet,
        onGoalClick = onGoalClick,
        onDeposit = { id, amount, note, force -> viewModel.deposit(id, amount, note, force) },
        onCreateGoal = { title, target, icon, color, startDate, targetDate -> viewModel.createGoal(title, target, icon, color, startDate, targetDate) },
        onDeleteGoal = { id -> viewModel.deleteGoal(id) }
    )
}

@Composable
fun GoalsScreenContent(
    uiState: GoalsUiState,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    showCreateBottomSheetExternal: Boolean = false,
    onDismissCreateBottomSheet: () -> Unit = {},
    onGoalClick: (String) -> Unit = {},
    onDeposit: (String, Double, String, Boolean) -> Unit = { _, _, _, _ -> },
    onCreateGoal: (String, Double, String, String, Long, Long) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteGoal: (String) -> Unit = {}
) {
    var showCreateBottomSheetInternal by remember { mutableStateOf(false) }
    var activeDepositGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    val showCreateBottomSheet = showCreateBottomSheetExternal || showCreateBottomSheetInternal

    goalToDelete?.let { goal ->
        DeleteGoalDialog(
            goalTitle = goal.title,
            onConfirm = { onDeleteGoal(goal.id); goalToDelete = null },
            onDismiss = { goalToDelete = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
    ) {
        when (uiState) {
            is GoalsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is GoalsUiState.Empty -> {
                EmptyGoalsContent(onCreateFirstGoalClick = { showCreateBottomSheetInternal = true })
            }
            is GoalsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GoalsUiState.Content -> {
                val allGoals = listOfNotNull(uiState.featuredGoal) + uiState.secondaryGoals
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    goalsListContent(
                        uiState = uiState,
                        allGoals = allGoals,
                        showOptionsMenu = showOptionsMenu,
                        onShowOptionsChange = { showOptionsMenu = it },
                        onCreateGoalClick = { showCreateBottomSheetInternal = true },
                        onGoalClick = { goal -> onGoalClick(goal.id) },
                        onGoalLongClick = { goalToDelete = it }
                    )
                }
            }
        }
    }

    if (showCreateBottomSheet) {
        CreateGoalBottomSheet(
            onDismissRequest = { showCreateBottomSheetInternal = false; onDismissCreateBottomSheet() },
            onConfirm = { title, target, icon, color, startDate, targetDate ->
                onCreateGoal(title, target, icon, color, startDate, targetDate)
                showCreateBottomSheetInternal = false
                onDismissCreateBottomSheet()
            }
        )
    }

    activeDepositGoal?.let { goal ->
        DepositBottomSheet(
            goalTitle = goal.title,
            onDismissRequest = { activeDepositGoal = null },
            onConfirm = { amount, note ->
                onDeposit(goal.id, amount, note, false)
                activeDepositGoal = null
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Goals Loading", showBackground = true)
@Composable
fun GoalsScreenLoadingPreview() {
    com.farbalapps.rinde.ui.theme.RindeTheme {
        GoalsScreenContent(uiState = GoalsUiState.Loading)
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Goals Empty Light", showBackground = true)
@Composable
fun GoalsScreenEmptyLightPreview() {
    com.farbalapps.rinde.ui.theme.RindeTheme {
        GoalsScreenContent(uiState = GoalsUiState.Empty)
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Goals Empty Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun GoalsScreenEmptyDarkPreview() {
    com.farbalapps.rinde.ui.theme.RindeTheme {
        GoalsScreenContent(uiState = GoalsUiState.Empty)
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Goals Content Light", showBackground = true)
@Composable
fun GoalsScreenContentLightPreview() {
    val mockGoal1 = SavingsGoal(
        id = "1",
        userId = "user1",
        title = "Fondo de Emergencia",
        targetAmount = 5000.0,
        currentAmount = 2500.0,
        targetDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
        iconKey = "savings",
        colorKey = "blue",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        monthlySnapshotAmount = 1000.0
    )
    val mockGoal2 = SavingsGoal(
        id = "2",
        userId = "user1",
        title = "Viaje a la Playa",
        targetAmount = 1500.0,
        currentAmount = 1500.0,
        targetDate = System.currentTimeMillis() + 15L * 24 * 60 * 60 * 1000,
        iconKey = "travel",
        colorKey = "green",
        isCompleted = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        monthlySnapshotAmount = 500.0
    )
    val mockState = GoalsUiState.Content(
        featuredGoal = mockGoal1,
        secondaryGoals = listOf(mockGoal2),
        canAddMore = true,
        summary = com.farbalapps.rinde.domain.usecase.goals.GoalsSummary(
            totalSaved = 4000.0,
            totalTarget = 6500.0,
            progressPercent = 61,
            monthlyGrowthPercent = 15
        )
    )
    com.farbalapps.rinde.ui.theme.RindeTheme {
        GoalsScreenContent(uiState = mockState)
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Goals Content Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun GoalsScreenContentDarkPreview() {
    val mockGoal1 = SavingsGoal(
        id = "1",
        userId = "user1",
        title = "Fondo de Emergencia",
        targetAmount = 5000.0,
        currentAmount = 2500.0,
        targetDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
        iconKey = "savings",
        colorKey = "blue",
        isCompleted = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        monthlySnapshotAmount = 1000.0
    )
    val mockState = GoalsUiState.Content(
        featuredGoal = mockGoal1,
        secondaryGoals = emptyList(),
        canAddMore = true,
        summary = com.farbalapps.rinde.domain.usecase.goals.GoalsSummary(
            totalSaved = 2500.0,
            totalTarget = 5000.0,
            progressPercent = 50,
            monthlyGrowthPercent = 10
        )
    )
    com.farbalapps.rinde.ui.theme.RindeTheme {
        GoalsScreenContent(uiState = mockState)
    }
}
@Composable
private fun DeleteGoalDialog(
    goalTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar Meta") },
        text = { Text("¿Estás seguro de que deseas eliminar la meta \"$goalTitle\"?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.goalsListContent(
    uiState: GoalsUiState.Content,
    allGoals: List<SavingsGoal>,
    showOptionsMenu: Boolean,
    onShowOptionsChange: (Boolean) -> Unit,
    onCreateGoalClick: () -> Unit,
    onGoalClick: (SavingsGoal) -> Unit,
    onGoalLongClick: (SavingsGoal) -> Unit
) {
    item {
        GoalsSummaryHeader(
            summary = uiState.summary,
            showOptionsMenu = showOptionsMenu,
            onShowOptionsChange = onShowOptionsChange,
            onCreateGoalClick = onCreateGoalClick
        )
    }
    item {
        ActiveGoalsHeader(
            canAddMore = uiState.canAddMore,
            onCreateGoalClick = onCreateGoalClick
        )
    }
    items(allGoals, key = { it.id }) { goal ->
        FeaturedGoalCard(
            goal = goal,
            onClick = { onGoalClick(goal) },
            onLongClick = { onGoalLongClick(goal) },
            onDeleteClick = { onGoalLongClick(goal) }
        )
    }
    item {
        Spacer(modifier = Modifier.height(4.dp))
        ChefRandomRecommendationCard()
    }
}

@Composable
private fun ActiveGoalsHeader(
    canAddMore: Boolean,
    onCreateGoalClick: () -> Unit
) {
    var showLimitDialog by remember { mutableStateOf(false) }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("Límite de metas alcanzado") },
            text = { Text("Actualmente solo puedes tener un máximo de 2 metas activas. Elimina una meta existente o espera a nuestras próximas actualizaciones para tener metas ilimitadas.") },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Objetivos activos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        TextButton(
            onClick = {
                if (canAddMore) onCreateGoalClick() else showLimitDialog = true
            }, 
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), 
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nueva Meta", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Nueva", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}
