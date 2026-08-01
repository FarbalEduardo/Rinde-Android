package com.farbalapps.rinde.ui.screen.home.goals.detail

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.ui.screen.home.goals.GoalsEvent
import com.farbalapps.rinde.ui.screen.home.goals.components.DepositBottomSheet
import com.farbalapps.rinde.ui.screen.home.goals.components.GoalThemeMapper
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

@Composable
fun GoalDetailScreen(
    onBack: () -> Unit,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDepositBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GoalsEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is GoalsEvent.ValidationError -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is GoalsEvent.GoalCompleted -> Toast.makeText(context, "🎉 ¡Felicidades! Completaste la meta: ${event.title}", Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    GoalDetailContent(
        uiState = uiState,
        onBack = onBack,
        onAddDepositClick = { showDepositBottomSheet = true },
        onEditGoalClick = {
            Toast.makeText(context, "Edición de meta disponible próximamente", Toast.LENGTH_SHORT).show()
        }
    )

    if (showDepositBottomSheet && uiState is GoalDetailUiState.Content) {
        val goal = (uiState as GoalDetailUiState.Content).goal
        DepositBottomSheet(
            goalTitle = goal.title,
            onDismissRequest = { showDepositBottomSheet = false },
            onConfirm = { amount, note ->
                viewModel.deposit(amount, note)
                showDepositBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailContent(
    uiState: GoalDetailUiState,
    onBack: () -> Unit,
    onAddDepositClick: () -> Unit,
    onEditGoalClick: () -> Unit
) {
    Scaffold(
        topBar = {
            GoalDetailTopAppBar(
                onBack = onBack,
                onEdit = onEditGoalClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is GoalDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GoalDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is GoalDetailUiState.Content -> {
                    GoalDetailBody(
                        content = uiState,
                        onAddDepositClick = onAddDepositClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailTopAppBar(
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Detalle de Meta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar meta",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun GoalDetailBody(
    content: GoalDetailUiState.Content,
    onAddDepositClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MainGoalCard(content = content)

        GridInfoCards(content = content)

        Spacer(modifier = Modifier.height(8.dp))

        ActionButtonsSection(
            onAddDepositClick = onAddDepositClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MainGoalCard(content: GoalDetailUiState.Content) {
    val goal = content.goal
    val themeColor = GoalThemeMapper.mapColor(goal.colorKey)
    val icon = GoalThemeMapper.mapIcon(goal.iconKey)
    val target = goal.targetAmount.coerceAtLeast(0.01)
    val fraction = (goal.currentAmount / target).coerceIn(0.0, 1.0).toFloat()
    val progressPercent = (fraction * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con ícono, nombre y categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(themeColor.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = goal.title,
                        tint = themeColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = content.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Medidor gráfico circular de porcentaje
            CircularProgressGauge(
                fraction = fraction,
                progressPercent = progressPercent,
                progressColor = themeColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(16.dp))

            // Fila de resumen de montos
            AmountRow(
                label = "Cantidad Ahorrada",
                amount = goal.currentAmount
            )

            Spacer(modifier = Modifier.height(12.dp))

            AmountRow(
                label = "Monto Meta",
                amount = goal.targetAmount
            )
        }
    }
}

@Composable
private fun CircularProgressGauge(
    fraction: Float,
    progressPercent: Int,
    progressColor: Color
) {
    val animatedProgress by animateFloatAsState(targetValue = fraction, label = "GaugeAnim")

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 18.dp.toPx()
            val trackColor = Color(0xFFE5E7EB)

            // Anillo base claro
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokeWidth)
            )

            // Arco de progreso
            val sweepAngle = animatedProgress * 360f
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$progressPercent%",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format(Locale.getDefault(), "$%,.2f", amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GridInfoCards(content: GoalDetailUiState.Content) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            InfoCard(
                icon = Icons.Default.CalendarToday,
                title = "CREACIÓN",
                value = content.formattedCreationDate,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Default.CalendarMonth,
                title = "LÍMITE",
                value = content.formattedLimitDate,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            InfoCard(
                icon = Icons.Default.AccountBalanceWallet,
                title = "APORTACIÓN",
                value = content.formattedMonthlyContribution,
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = Icons.Default.History,
                title = "ÚLTIMO",
                value = content.formattedLastContribution,
                isItalic = content.formattedLastContribution.contains("No"),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    isItalic: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF6F8FD)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
                ),
                fontWeight = if (isItalic) FontWeight.Normal else FontWeight.Bold,
                color = if (isItalic) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onAddDepositClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onAddDepositClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0066FF)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Agregar a la meta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Goal Detail Light", showBackground = true)
@Composable
fun GoalDetailContentLightPreview() {
    val mockGoal = SavingsGoal(
        id = "1",
        userId = "user1",
        title = "Fondo de Emergencia",
        targetAmount = 5000.0,
        currentAmount = 3200.0,
        targetDate = System.currentTimeMillis() + 45L * 24 * 60 * 60 * 1000,
        iconKey = "savings",
        colorKey = "blue",
        isCompleted = false,
        createdAt = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000,
        updatedAt = System.currentTimeMillis(),
        monthlySnapshotAmount = 1000.0
    )
    val mockContent = GoalDetailUiState.Content(
        goal = mockGoal,
        transactions = emptyList(),
        categoryName = "General",
        formattedCreationDate = "15 Ene 2026",
        formattedLimitDate = "15 Mar 2026",
        formattedMonthlyContribution = "$1,200.00 / mes",
        formattedLastContribution = "$300.00 (Ayer)"
    )
    com.farbalapps.rinde.ui.theme.RindeTheme {
        GoalDetailContent(
            uiState = mockContent,
            onBack = {},
            onAddDepositClick = {},
            onEditGoalClick = {}
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Goal Detail Dark",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun GoalDetailContentDarkPreview() {
    val mockGoal = SavingsGoal(
        id = "1",
        userId = "user1",
        title = "Nuevo Auto",
        targetAmount = 10000.0,
        currentAmount = 7500.0,
        targetDate = System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000,
        iconKey = "car",
        colorKey = "blue",
        isCompleted = false,
        createdAt = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
        updatedAt = System.currentTimeMillis(),
        monthlySnapshotAmount = 2000.0
    )
    val mockContent = GoalDetailUiState.Content(
        goal = mockGoal,
        transactions = emptyList(),
        categoryName = "Vehículo",
        formattedCreationDate = "01 Ene 2026",
        formattedLimitDate = "01 Abr 2026",
        formattedMonthlyContribution = "$2,500.00 / mes",
        formattedLastContribution = "$500.00 (Hace 3 días)"
    )
    com.farbalapps.rinde.ui.theme.RindeTheme {
        GoalDetailContent(
            uiState = mockContent,
            onBack = {},
            onAddDepositClick = {},
            onEditGoalClick = {}
        )
    }
}
