package com.farbalapps.rinde.ui.screen.home.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.ui.screen.home.dashboard.components.ExtraExpenseBottomSheet
import com.farbalapps.rinde.ui.screen.home.dashboard.components.ExtraExpenseEditBottomSheet
import com.farbalapps.rinde.ui.screen.home.dashboard.components.FinancialHealthCard
import com.farbalapps.rinde.ui.screen.home.dashboard.components.GoalsMiniSection
import com.farbalapps.rinde.ui.screen.home.dashboard.components.IncomeSetupBottomSheet
import com.farbalapps.rinde.ui.screen.home.dashboard.components.LockedSavingsCard
import com.farbalapps.rinde.ui.screen.home.dashboard.components.SetupIncomeCard
import com.farbalapps.rinde.ui.theme.RindeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onGoalClick: (String) -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onGoalClick = onGoalClick,
        onNavigateToGoals = onNavigateToGoals,
        onCardClick = onCardClick,
        modifier = modifier,
        onSetupIncomeClick = { viewModel.openIncomeSheet() },
        onEditIncome = { viewModel.openIncomeSheet() },
        onAddExpense = { viewModel.openExpenseSheet() },
        onEditExpense = { expense -> viewModel.openEditExpenseSheet(expense) },
        onDeleteExpense = { expenseId -> viewModel.deleteExtraExpense(expenseId) },
        onCloseIncomeSheet = { viewModel.closeIncomeSheet() },
        onSaveIncome = { amount, frequency, start, end -> viewModel.saveIncome(amount, frequency, "MXN", start, end) },
        onCloseExpenseSheet = { viewModel.closeExpenseSheet() },
        onAddExtraExpense = { label, amount, iconKey -> viewModel.addExtraExpense(label, amount, iconKey) },
        onCloseEditExpenseSheet = { viewModel.closeEditExpenseSheet() },
        onUpdateExtraExpense = { id, label, amount -> viewModel.updateExtraExpense(id, label, amount) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onGoalClick: (String) -> Unit,
    onNavigateToGoals: () -> Unit,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onSetupIncomeClick: () -> Unit = {},
    onEditIncome: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onEditExpense: (ExtraExpense) -> Unit = {},
    onDeleteExpense: (String) -> Unit = {},
    onCloseIncomeSheet: () -> Unit = {},
    onSaveIncome: (Double, com.farbalapps.rinde.domain.model.IncomeFrequency, Long?, Long?) -> Unit = { _, _, _, _ -> },
    onCloseExpenseSheet: () -> Unit = {},
    onAddExtraExpense: (String, Double, String) -> Unit = { _, _, _ -> },
    onCloseEditExpenseSheet: () -> Unit = {},
    onUpdateExtraExpense: (String, String, Double) -> Unit = { _, _, _ -> }
) {
    val scrollState = rememberScrollState()

    // El card de ahorros se mantiene listo pero oculto según la especificación
    val showSavingsCard = false

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Header: Título "Rinde", Subtítulo y Chip del Mes Activo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.dashboard_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.dashboard_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Selector / Chip Informativo del Periodo Actual
                if (uiState.currentMonthName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.currentMonthName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Principal: Salud Financiera (o Skeleton si está cargando, o Setup si no tiene ingreso)
            if (uiState.isLoading) {
                DashboardCardSkeleton()
            } else if (!uiState.hasIncomeConfigured) {
                SetupIncomeCard(
                    onSetupIncomeClick = onSetupIncomeClick
                )
            } else {
                FinancialHealthCard(
                    uiState = uiState,
                    onEditIncome = onEditIncome,
                    onAddExpense = onAddExpense,
                    onDeleteExpense = onDeleteExpense,
                    onEditExpense = onEditExpense,
                    onCardClick = onCardClick
                )
            }

            // Card de Ahorro Rinde (mantenida oculta hasta activación futura)
            if (showSavingsCard) {
                Spacer(modifier = Modifier.height(12.dp))
                LockedSavingsCard()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sección de Metas Activas
            GoalsMiniSection(
                goals = uiState.activeGoals,
                onGoalClick = onGoalClick,
                onNavigateToGoals = onNavigateToGoals
            )

            // Espaciador inferior para que el último elemento libre la BottomNavigationBar de forma limpia
            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 8.dp))
        }

        // BottomSheet para configurar o editar ingreso
        if (uiState.isIncomeSheetOpen) {
            IncomeSetupBottomSheet(
                currentProfile = uiState.profile,
                onDismiss = onCloseIncomeSheet,
                onSave = onSaveIncome
            )
        }

        // BottomSheet para agregar un gasto extra del hogar
        if (uiState.isExpenseSheetOpen) {
            ExtraExpenseBottomSheet(
                onDismiss = onCloseExpenseSheet,
                onSave = onAddExtraExpense
            )
        }

        // BottomSheet para editar o eliminar un gasto extra existente
        if (uiState.isEditExpenseSheetOpen && uiState.editingExpense != null) {
            ExtraExpenseEditBottomSheet(
                expense = uiState.editingExpense,
                onDismiss = onCloseEditExpenseSheet,
                onUpdate = onUpdateExtraExpense,
                onDelete = onDeleteExpense
            )
        }
    }
}

@Composable
private fun DashboardCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier.width(130.dp).height(22.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {}
                Surface(
                    modifier = Modifier.width(70.dp).height(22.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {}
            }
            Surface(
                modifier = Modifier.fillMaxWidth().height(14.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {}
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {}
                Surface(
                    modifier = Modifier.fillMaxWidth(0.65f).height(12.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ) {}
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    RindeTheme {
        DashboardContent(
            uiState = DashboardUiState(
                currentMonthName = "Septiembre",
                monthlyIncome = 15000.0,
                profile = com.farbalapps.rinde.domain.model.FinancialProfile(
                    id = "1",
                    income = 15000.0,
                    incomeFrequency = com.farbalapps.rinde.domain.model.IncomeFrequency.MONTHLY,
                    currency = "MXN"
                )
            ),
            onGoalClick = {},
            onNavigateToGoals = {}
        )
    }
}
