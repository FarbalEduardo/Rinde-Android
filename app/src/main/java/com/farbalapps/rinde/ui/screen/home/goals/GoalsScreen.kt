package com.farbalapps.rinde.ui.screen.home.goals

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
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
    val archivedGoals by viewModel.archivedGoals.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GoalsEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is GoalsEvent.ValidationError -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is GoalsEvent.GoalLimitReached -> Toast.makeText(context, "Límite de metas alcanzado (máximo 2).", Toast.LENGTH_SHORT).show()
                is GoalsEvent.GoalCompleted -> Toast.makeText(context, "🎉 ¡Felicidades! Completaste la meta: ${event.title}", Toast.LENGTH_LONG).show()
                is GoalsEvent.DepositExceedsTarget -> { }
            }
        }
    }

    GoalsScreenContent(
        uiState = uiState,
        archivedGoals = archivedGoals,
        innerPadding = innerPadding,
        showCreateBottomSheetExternal = showCreateBottomSheetExternal,
        onDismissCreateBottomSheet = onDismissCreateBottomSheet,
        onGoalClick = onGoalClick,
        onDeposit = { id, amount, note, force -> viewModel.deposit(id, amount, note, force) },
        onCreateGoal = { title, target, icon, color, startDate, targetDate -> viewModel.createGoal(title, target, icon, color, startDate, targetDate) },
        onDeleteGoal = { id -> viewModel.deleteGoal(id) },
        onArchiveGoal = { id -> viewModel.archiveGoal(id) },
        onTogglePrivacyMode = { viewModel.togglePrivacyMode(it) },
        onToggleReorderMode = { viewModel.toggleReorderMode() },
        onReorderGoals = { goals -> viewModel.saveGoalOrder(goals) }
    )
}

@Composable
fun GoalsScreenContent(
    uiState: GoalsUiState,
    archivedGoals: List<SavingsGoal> = emptyList(),
    innerPadding: PaddingValues = PaddingValues(0.dp),
    showCreateBottomSheetExternal: Boolean = false,
    onDismissCreateBottomSheet: () -> Unit = {},
    onGoalClick: (String) -> Unit = {},
    onDeposit: (String, Double, String, Boolean) -> Unit = { _, _, _, _ -> },
    onCreateGoal: (String, Double, String, String, Long, Long) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteGoal: (String) -> Unit = {},
    onArchiveGoal: (String) -> Unit = {},
    onTogglePrivacyMode: (Boolean) -> Unit = {},
    onToggleReorderMode: () -> Unit = {},
    onReorderGoals: (List<SavingsGoal>) -> Unit = {}
) {
    var showCreateBottomSheetInternal by remember { mutableStateOf(false) }
    var showArchivedGoalsModal by remember { mutableStateOf(false) }
    var activeDepositGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoal?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    val showCreateBottomSheet = showCreateBottomSheetExternal || showCreateBottomSheetInternal

    val isReorderModeActive = (uiState as? GoalsUiState.Content)?.isReorderMode == true
    BackHandler(enabled = isReorderModeActive) {
        onToggleReorderMode()
    }

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

    goalToDelete?.let { goal ->
        DeleteGoalDialog(
            goalTitle = goal.title,
            onConfirm = { onDeleteGoal(goal.id); goalToDelete = null },
            onDismiss = { goalToDelete = null }
        )
    }

    if (showArchivedGoalsModal) {
        ArchivedGoalsModal(
            archivedGoals = archivedGoals,
            onDismissRequest = { showArchivedGoalsModal = false },
            onDeleteGoal = { id -> onDeleteGoal(id) }
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
                EmptyGoalsContent(
                    onCreateFirstGoalClick = { showCreateBottomSheetInternal = true },
                    onShowArchivedGoalsClick = { showArchivedGoalsModal = true }
                )
            }
            is GoalsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GoalsUiState.Content -> {
                goalsListContent(
                    uiState = uiState,
                    showOptionsMenu = showOptionsMenu,
                    onShowOptionsChange = { showOptionsMenu = it },
                    onCreateGoalClick = { showCreateBottomSheetInternal = true },
                    onGoalClick = { goal -> onGoalClick(goal.id) },
                    onGoalLongClick = { goalToDelete = it },
                    onArchiveGoal = { goal -> onArchiveGoal(goal.id) },
                    onTogglePrivacyMode = onTogglePrivacyMode,
                    onToggleReorderMode = onToggleReorderMode,
                    onShowArchivedGoals = { showArchivedGoalsModal = true },
                    onReorderGoals = onReorderGoals,
                    onShowLimitDialog = { showLimitDialog = true }
                )
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

@Composable
private fun goalsListContent(
    uiState: GoalsUiState.Content,
    showOptionsMenu: Boolean,
    onShowOptionsChange: (Boolean) -> Unit,
    onCreateGoalClick: () -> Unit,
    onGoalClick: (SavingsGoal) -> Unit,
    onGoalLongClick: (SavingsGoal) -> Unit,
    onArchiveGoal: (SavingsGoal) -> Unit,
    onTogglePrivacyMode: (Boolean) -> Unit,
    onToggleReorderMode: () -> Unit,
    onShowArchivedGoals: () -> Unit,
    onShowLimitDialog: () -> Unit,
    onReorderGoals: (List<SavingsGoal>) -> Unit
) {
    val allGoalsOriginal = listOfNotNull(uiState.featuredGoal) + uiState.secondaryGoals
    var allGoals by remember(allGoalsOriginal) { mutableStateOf(allGoalsOriginal) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            GoalsSummaryHeader(
                summary = uiState.summary,
                isPrivacyMode = uiState.isPrivacyMode,
                onTogglePrivacyMode = onTogglePrivacyMode,
                showOptionsMenu = showOptionsMenu,
                onShowOptionsChange = onShowOptionsChange,
                onCreateGoalClick = onCreateGoalClick
            )
        }
        item {
            Spacer(modifier = Modifier.height(10.dp))
            ChefRandomRecommendationCard()
        }
        item {
            ActiveGoalsHeader(
                canAddMore = uiState.canAddMore,
                isReorderMode = uiState.isReorderMode,
                onToggleReorderMode = onToggleReorderMode,
                onShowArchivedGoals = onShowArchivedGoals,
                onCreateGoalClick = onCreateGoalClick,
                showLimitDialogEvent = onShowLimitDialog
            )
        }
        item {
            DraggableGoalsList(
                goals = allGoals,
                isDragEnabled = uiState.isReorderMode,
                onOrderChange = { newList ->
                    allGoals = newList
                    onReorderGoals(newList)
                }
            ) { goal, isDragging, dragModifier ->
                FeaturedGoalCard(
                    goal = goal,
                    isPrivacyMode = uiState.isPrivacyMode,
                    onClick = { onGoalClick(goal) },
                    onLongClick = { onGoalLongClick(goal) },
                    onEditClick = { onGoalClick(goal) },
                    onDeleteClick = { onGoalLongClick(goal) },
                    onArchiveClick = { onArchiveGoal(goal) },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .then(dragModifier)
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            AddGoalCard(
                onClick = {
                    if (uiState.canAddMore) {
                        onCreateGoalClick()
                    } else {
                        onShowLimitDialog()
                    }
                }
            )
        }
    }
}

@Composable
private fun ActiveGoalsHeader(
    canAddMore: Boolean,
    isReorderMode: Boolean,
    onToggleReorderMode: () -> Unit,
    onShowArchivedGoals: () -> Unit,
    onCreateGoalClick: () -> Unit,
    showLimitDialogEvent: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Objetivos activos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onToggleReorderMode, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isReorderMode) "Guardar orden" else "Reordenar metas",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onShowArchivedGoals, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Historial de metas archivadas",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
