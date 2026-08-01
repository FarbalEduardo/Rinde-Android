package com.farbalapps.rinde.ui.screen.home.goals.detail

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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

import com.farbalapps.rinde.ui.screen.home.goals.components.CreateGoalBottomSheet

@Composable
fun GoalDetailScreen(
    onBack: () -> Unit,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDepositBottomSheet by remember { mutableStateOf(false) }
    var showEditGoalBottomSheet by remember { mutableStateOf(false) }

    var excessEvent by remember { mutableStateOf<GoalsEvent.DepositExceedsTarget?>(null) }

    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is GoalsEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is GoalsEvent.ValidationError -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                is GoalsEvent.GoalCompleted -> Toast.makeText(context, "🎉 ¡Felicidades! Completaste la meta: ${event.title}", Toast.LENGTH_LONG).show()
                is GoalsEvent.DepositExceedsTarget -> {
                    excessEvent = event
                }
                else -> {}
            }
        }
    }

    GoalDetailContent(
        uiState = uiState,
        onBack = onBack,
        onAddDepositClick = { showDepositBottomSheet = true },
        onEditGoalClick = { showEditGoalBottomSheet = true }
    )

    if (showEditGoalBottomSheet && uiState is GoalDetailUiState.Content) {
        val goal = (uiState as GoalDetailUiState.Content).goal
        CreateGoalBottomSheet(
            initialGoal = goal,
            onDismissRequest = { showEditGoalBottomSheet = false },
            onConfirm = { title, targetAmount, iconKey, colorKey, startDate, targetDate ->
                viewModel.updateGoal(title, targetAmount, iconKey, colorKey, startDate, targetDate)
                showEditGoalBottomSheet = false
            }
        )
    }

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

    excessEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { excessEvent = null },
            title = { Text("El abono supera la meta") },
            text = {
                Text(
                    String.format(
                        Locale.getDefault(),
                        "Este abono de $%,.2f supera el objetivo por $%,.2f. ¿Deseas agregarlo de todas formas? La meta se completará y el objetivo se actualizará al valor excedente.",
                        event.amount,
                        event.excess
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deposit(event.amount, event.note, force = true)
                        excessEvent = null
                    }
                ) {
                    Text("Sí, abonar")
                }
            },
            dismissButton = {
                TextButton(onClick = { excessEvent = null }) {
                    Text("Cancelar")
                }
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
        bottomBar = {
            if (uiState is GoalDetailUiState.Content) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = onAddDepositClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color.White.copy(alpha = 0.25f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Agregar a la meta",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
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
                    GoalDetailBody(content = uiState)
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
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = CircleShape)
            ) {
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun GoalDetailBody(
    content: GoalDetailUiState.Content
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MainGoalCard(content = content)

        GridInfoCards(content = content)

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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con ícono, nombre y categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(themeColor.copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = goal.title,
                        tint = themeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = themeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = content.categoryName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gauge circular de porcentaje
            CircularProgressGauge(
                fraction = fraction,
                progressPercent = progressPercent,
                progressColor = themeColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Fila inferior de Ahorrado y Meta Total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AHORRADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9EA7BA),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "$%,.2f", goal.currentAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(36.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "META TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9EA7BA),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "$%,.2f", goal.targetAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
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
        modifier = Modifier.size(175.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 15.dp.toPx()
            val trackColor = Color(0xFFEBF1F9)

            // Anillo base
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "PROGRESO",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9EA7BA),
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
private fun GridInfoCards(content: GoalDetailUiState.Content) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GridInfoItem(
                    icon = Icons.Default.CalendarToday,
                    iconTint = Color(0xFF1976D2),
                    title = "CREACIÓN",
                    value = content.formattedCreationDate,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )

                GridInfoItem(
                    icon = Icons.Default.Schedule,
                    iconTint = Color(0xFFF57C00),
                    title = "LÍMITE",
                    value = content.formattedLimitDate,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GridInfoItem(
                    icon = Icons.Default.Payments,
                    iconTint = Color(0xFF388E3C),
                    title = "APORTACIÓN",
                    value = content.formattedMonthlyContribution,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                )

                GridInfoItem(
                    icon = Icons.Default.Refresh,
                    iconTint = Color(0xFF7B1FA2),
                    title = "ÚLTIMO",
                    value = content.formattedLastContribution,
                    isItalic = content.formattedLastContribution.contains("No"),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GridInfoItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    isItalic: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9EA7BA),
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
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
