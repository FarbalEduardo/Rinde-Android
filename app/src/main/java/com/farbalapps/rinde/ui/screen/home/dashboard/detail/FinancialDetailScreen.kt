package com.farbalapps.rinde.ui.screen.home.dashboard.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.farbalapps.rinde.domain.model.ExtraIncome
import com.farbalapps.rinde.ui.screen.home.dashboard.components.ExtraIncomeBottomSheet
import com.farbalapps.rinde.ui.screen.home.dashboard.components.ExtraIncomeEditBottomSheet
import com.farbalapps.rinde.ui.screen.home.dashboard.components.IncomeSetupBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.FinancialPeriod
import com.farbalapps.rinde.domain.model.PeriodType
import com.farbalapps.rinde.ui.screen.home.dashboard.FinancialHealthStatus
import com.farbalapps.rinde.ui.theme.RindeTheme
import androidx.compose.ui.tooling.preview.Preview
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDetailScreen(
    onBack: () -> Unit,
    onNavigateToGoals: () -> Unit = {},
    onNavigateToList: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FinancialDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val cal = remember(uiState.selectedPeriod) {
        java.util.Calendar.getInstance().apply { timeInMillis = uiState.selectedPeriod.startTimestamp }
    }
    val currentYear = if (uiState.selectedPeriod is com.farbalapps.rinde.domain.model.FinancialPeriod.Month) {
        (uiState.selectedPeriod as com.farbalapps.rinde.domain.model.FinancialPeriod.Month).year
    } else cal.get(java.util.Calendar.YEAR)
    val currentMonth = if (uiState.selectedPeriod is com.farbalapps.rinde.domain.model.FinancialPeriod.Month) {
        (uiState.selectedPeriod as com.farbalapps.rinde.domain.model.FinancialPeriod.Month).month
    } else (cal.get(java.util.Calendar.MONTH) + 1)

    FinancialDetailContent(
        uiState = uiState,
        onBack = onBack,
        onNavigateToGoals = onNavigateToGoals,
        onNavigateToList = onNavigateToList,
        onPeriodTypeSelected = { viewModel.setPeriodType(it) },
        onPreviousPeriod = { viewModel.navigatePrevious() },
        onNextPeriod = { viewModel.navigateNext() },
        onOpenCalendar = { viewModel.openDatePicker() },
        onOpenFinancialCalendar = { viewModel.openFinancialCalendar() },
        onAddExpense = { viewModel.openAddExpenseSheet() },
        onEditExpense = { viewModel.openEditExpense(it) },
        onEditIncome = { viewModel.openIncomeSheet() },
        onDeleteSalaryClick = { viewModel.openDeleteSalaryDialog(currentYear, currentMonth) },
        onAddExtraIncome = { viewModel.openAddExtraIncomeSheet() },
        onEditExtraIncome = { viewModel.openEditExtraIncome(it) },
        onDeleteExtraIncome = { viewModel.deleteExtraIncome(it) },
        modifier = modifier
    )

    // Modal selector de un solo día
    if (uiState.showDatePickerModal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedPeriod.startTimestamp.coerceAtMost(System.currentTimeMillis()),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.closeDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateSelected(it) }
                }) { Text("Seleccionar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDatePicker() }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Modal selector de rango de fechas para Personalizado
    if (uiState.showDateRangePickerModal) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = uiState.selectedPeriod.startTimestamp.coerceAtMost(System.currentTimeMillis()),
            initialSelectedEndDateMillis = uiState.selectedPeriod.endTimestamp.coerceAtMost(System.currentTimeMillis()),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.closeDateRangePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        if (start != null) {
                            val end = dateRangePickerState.selectedEndDateMillis ?: start
                            viewModel.onCustomRangeSelected(start, end)
                        }
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null
                ) { Text("Aplicar Rango") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDateRangePicker() }) { Text("Cancelar") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Selecciona el rango de fechas",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Modal para agregar un gasto extra del hogar desde Detalle Financiero
    if (uiState.isAddExpenseSheetOpen) {
        com.farbalapps.rinde.ui.screen.home.dashboard.components.ExtraExpenseBottomSheet(
            onDismiss = { viewModel.closeAddExpenseSheet() },
            onSave = { label, amount, iconKey, expenseDate ->
                viewModel.addExtraExpense(label, amount, iconKey, expenseDate)
            }
        )
    }

    // Modal para editar o eliminar un gasto extra del hogar
    val currentEditingExpense = uiState.editingExpense
    if (uiState.isEditExpenseSheetOpen && currentEditingExpense != null) {
        com.farbalapps.rinde.ui.screen.home.dashboard.components.ExtraExpenseEditBottomSheet(
            expense = currentEditingExpense,
            onDismiss = { viewModel.closeEditExpense() },
            onUpdate = { id, label, amount, expenseDate ->
                viewModel.updateExtraExpense(id, label, amount, expenseDate)
            },
            onDelete = { id -> viewModel.deleteExtraExpense(id) }
        )
    }

    // Calendario Financiero interactivo de pagos y gastos
    if (uiState.isFinancialCalendarOpen) {
        val cal = remember(uiState.selectedPeriod) {
            java.util.Calendar.getInstance().apply { timeInMillis = uiState.selectedPeriod.startTimestamp }
        }
        val year = if (uiState.selectedPeriod is com.farbalapps.rinde.domain.model.FinancialPeriod.Month) {
            (uiState.selectedPeriod as com.farbalapps.rinde.domain.model.FinancialPeriod.Month).year
        } else cal.get(java.util.Calendar.YEAR)
        val month = if (uiState.selectedPeriod is com.farbalapps.rinde.domain.model.FinancialPeriod.Month) {
            (uiState.selectedPeriod as com.farbalapps.rinde.domain.model.FinancialPeriod.Month).month
        } else (cal.get(java.util.Calendar.MONTH) + 1)

        FinancialCalendarModal(
            initialYear = year,
            initialMonth = month,
            extraExpenses = uiState.extraExpenses,
            extraIncomes = uiState.extraIncomes,
            profile = uiState.profile,
            currency = uiState.currency,
            onDismiss = { viewModel.closeFinancialCalendar() },
            onDeleteSalarySingleMonth = { y, m -> viewModel.deleteSalarySingleMonth(y, m) },
            onDeleteSalaryFutureMonths = { y, m -> viewModel.deleteSalaryFutureMonths(y, m) },
            onDeleteSalaryAll = { viewModel.deleteSalaryAll() }
        )
    }

    // Modal para configurar o modificar ingreso del periodo
    if (uiState.isIncomeSheetOpen) {
        val currentPeriod = uiState.selectedPeriod
        val cal = remember(currentPeriod) {
            java.util.Calendar.getInstance().apply { timeInMillis = currentPeriod.startTimestamp }
        }
        val sdf = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("es", "ES")) }
        val monthName = sdf.format(cal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }

        IncomeSetupBottomSheet(
            currentProfile = uiState.profile,
            currentMonthName = monthName,
            onDismiss = { viewModel.closeIncomeSheet() },
            onSave = { amount, frequency, startDate, endDate, isVariable, paymentDate ->
                viewModel.saveIncome(
                    income = amount,
                    frequency = frequency,
                    customStartDate = startDate,
                    customEndDate = endDate,
                    isVariable = isVariable,
                    paymentDate = paymentDate
                )
            }
        )
    }

    // Modal para agregar una ganancia extra
    if (uiState.isAddIncomeSheetOpen) {
        ExtraIncomeBottomSheet(
            onDismiss = { viewModel.closeAddExtraIncomeSheet() },
            onSave = { label, amount, iconKey, incomeDate ->
                viewModel.addExtraIncome(label, amount, iconKey, incomeDate)
            }
        )
    }

    // Modal para editar o eliminar una ganancia extra
    val editingIncome = uiState.editingIncome
    if (uiState.isEditIncomeSheetOpen && editingIncome != null) {
        ExtraIncomeEditBottomSheet(
            income = editingIncome,
            onDismiss = { viewModel.closeEditExtraIncome() },
            onUpdate = { id, label, amount, incomeDate ->
                viewModel.updateExtraIncome(id, label, amount, incomeDate)
            },
            onDelete = { id ->
                viewModel.deleteExtraIncome(id)
            }
        )
    }

    // Diálogo con 3 opciones para eliminar sueldo fijo o recurrente
    if (uiState.isDeleteSalaryDialogOpen) {
        var selectedDeleteOption by remember { mutableIntStateOf(1) }
        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteSalaryDialog() },
            title = {
                Text(
                    text = stringResource(id = R.string.financial_calendar_dialog_delete_salary_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.financial_calendar_dialog_delete_salary_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DeleteSalaryOptionItem(
                        title = stringResource(id = R.string.financial_calendar_opt_only_this),
                        description = stringResource(id = R.string.financial_calendar_opt_only_this_desc),
                        isSelected = selectedDeleteOption == 1,
                        onClick = { selectedDeleteOption = 1 }
                    )

                    DeleteSalaryOptionItem(
                        title = stringResource(id = R.string.financial_calendar_opt_this_and_future),
                        description = stringResource(id = R.string.financial_calendar_opt_this_and_future_desc),
                        isSelected = selectedDeleteOption == 2,
                        onClick = { selectedDeleteOption = 2 }
                    )

                    DeleteSalaryOptionItem(
                        title = stringResource(id = R.string.financial_calendar_opt_all),
                        description = stringResource(id = R.string.financial_calendar_opt_all_desc),
                        isSelected = selectedDeleteOption == 3,
                        onClick = { selectedDeleteOption = 3 }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val year = uiState.pendingDeleteSalaryYear
                        val month = uiState.pendingDeleteSalaryMonth
                        when (selectedDeleteOption) {
                            1 -> viewModel.deleteSalarySingleMonth(year, month)
                            2 -> viewModel.deleteSalaryFutureMonths(year, month)
                            3 -> viewModel.deleteSalaryAll()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = stringResource(id = R.string.financial_calendar_delete_salary_confirm_btn),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDeleteSalaryDialog() }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun PeriodTypeSelectorRow(
    currentType: PeriodType,
    onSelectType: (PeriodType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PeriodTabChip(
            title = "Mes",
            isSelected = currentType == PeriodType.MONTH,
            onClick = { onSelectType(PeriodType.MONTH) },
            modifier = Modifier.weight(1f)
        )
        PeriodTabChip(
            title = "Quincena",
            isSelected = currentType == PeriodType.FORTNIGHT,
            onClick = { onSelectType(PeriodType.FORTNIGHT) },
            modifier = Modifier.weight(1.1f)
        )
        PeriodTabChip(
            title = "Semana",
            isSelected = currentType == PeriodType.WEEK,
            onClick = { onSelectType(PeriodType.WEEK) },
            modifier = Modifier.weight(1f)
        )
        PeriodTabChip(
            title = "Día",
            isSelected = currentType == PeriodType.DAY,
            onClick = { onSelectType(PeriodType.DAY) },
            modifier = Modifier.weight(0.9f)
        )
        PeriodTabChip(
            title = "🗓️ Libre",
            isSelected = currentType == PeriodType.CUSTOM,
            onClick = { onSelectType(PeriodType.CUSTOM) },
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
private fun PeriodTabChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun PeriodNavigatorBar(
    displayName: String,
    canNavigateNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior", tint = MaterialTheme.colorScheme.primary)
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenCalendar)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Cambiar fecha",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onNext,
                enabled = canNavigateNext,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Siguiente",
                    tint = if (canNavigateNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
private fun PeriodSummaryHeroCard(
    uiState: FinancialDetailUiState,
    onOpenCalendar: () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.expenseFraction,
        animationSpec = tween(durationMillis = 500),
        label = "hero_progress"
    )

    val (statusColor, statusText) = when (uiState.healthStatus) {
        FinancialHealthStatus.GOOD -> Pair(Color(0xFF66BB6A), "Salud Financiera: Óptima")
        FinancialHealthStatus.WARNING -> Pair(Color(0xFFFFA726), "Salud Financiera: Atención")
        FinancialHealthStatus.CRITICAL -> Pair(Color(0xFFEF5350), "Salud Financiera: Crítica")
        FinancialHealthStatus.SETUP_REQUIRED -> Pair(Color(0xFF26A69A), "Salud Financiera: Configurar")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESUMEN DEL PERIODO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )

                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(statusColor, CircleShape))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val availableFormatted = String.format(Locale.getDefault(), "$%,.0f", uiState.availableAmount)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = availableFormatted,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = if (uiState.availableAmount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${uiState.currency} · Disponible neto para este periodo (${uiState.selectedPeriod.durationDays} días)",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de progreso Gastos vs Libre
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gastos: ${uiState.expensePercentage}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "Libre: ${uiState.freePercentage}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            // Banner interactivo para abrir Calendario Financiero
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenCalendar)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.financial_detail_calendar_btn),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Comparativa de 3 columnas (Ingreso, Gastos, Balance)
            val incomeSubtitle = if (uiState.extraIncomesTotal > 0.0) {
                "Base $%,.0f • Extras +$%,.0f".format(Locale.getDefault(), uiState.basePeriodIncome, uiState.proportionalExtraIncome)
            } else null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem(
                    label = "Ingreso periodo",
                    amount = String.format(Locale.getDefault(), "$%,.0f", uiState.periodIncome),
                    color = Color(0xFF66BB6A),
                    subtitle = incomeSubtitle
                )
                SummaryMetricItem(
                    label = "Gastos totales",
                    amount = String.format(Locale.getDefault(), "$%,.0f", uiState.totalExpenses),
                    color = Color(0xFFEF5350)
                )
                SummaryMetricItem(
                    label = "Disponible",
                    amount = String.format(Locale.getDefault(), "$%,.0f", uiState.availableAmount),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    amount: String,
    color: Color,
    subtitle: String? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PeriodIncomeDetailCard(
    uiState: FinancialDetailUiState,
    onEditIncome: () -> Unit = {},
    onDeleteSalaryClick: () -> Unit = {},
    onAddExtraIncome: () -> Unit = {},
    onEditExtraIncome: (ExtraIncome) -> Unit = {},
    onDeleteExtraIncome: (String) -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Cabecera con monto total proporcional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFF66BB6A),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Ingreso Proporcional",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Base calculada para ${uiState.selectedPeriod.durationDays} días",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = String.format(Locale.getDefault(), "$%,.0f MXN", uiState.periodIncome),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF66BB6A)
                )
            }

            if (uiState.isVariableIncome) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = stringResource(id = R.string.financial_detail_income_variable_notice),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Bloque: Sueldo Principal
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = stringResource(id = R.string.financial_detail_main_income_title),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val subtitleText = if (uiState.isVariableIncome) {
                                stringResource(id = R.string.financial_detail_main_income_subtitle_var)
                            } else {
                                val freqText = when (uiState.profile?.incomeFrequency) {
                                    com.farbalapps.rinde.domain.model.IncomeFrequency.MONTHLY -> "Mensual"
                                    com.farbalapps.rinde.domain.model.IncomeFrequency.BIWEEKLY -> "Quincenal"
                                    com.farbalapps.rinde.domain.model.IncomeFrequency.WEEKLY -> "Semanal"
                                    com.farbalapps.rinde.domain.model.IncomeFrequency.DAILY -> "Diario"
                                    com.farbalapps.rinde.domain.model.IncomeFrequency.CUSTOM -> "Personalizado"
                                    null -> "No configurado"
                                }
                                "${stringResource(id = R.string.financial_detail_main_income_subtitle_fixed)} ($freqText: $${String.format(Locale.getDefault(), "%,.0f", uiState.profile?.income ?: 0.0)})"
                            }
                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "+$${String.format(Locale.getDefault(), "%,.0f", uiState.basePeriodIncome)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )

                        IconButton(
                            onClick = onEditIncome,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Modificar sueldo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        if (uiState.basePeriodIncome > 0.0 || (uiState.profile != null && uiState.profile.income > 0.0)) {
                            IconButton(
                                onClick = onDeleteSalaryClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Eliminar sueldo",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Bloque: Pagos y Ganancias Extras
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.financial_detail_extra_incomes_section),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (uiState.extraIncomesTotal > 0.0) {
                    Text(
                        text = "+$${String.format(Locale.getDefault(), "%,.0f", uiState.proportionalExtraIncome)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.extraIncomes.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.financial_detail_no_extra_incomes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
                    uiState.extraIncomes.forEach { income ->
                        val incomeDateMillis = if (income.incomeDate > 0L) income.incomeDate else income.createdAt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEditExtraIncome(income) }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = income.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = dateFmt.format(Date(incomeDateMillis)),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "+ $${String.format(Locale.getDefault(), "%,.0f", income.amount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF2E7D32)
                                )

                                IconButton(
                                    onClick = { onDeleteExtraIncome(income.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Botones de acción al pie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddExtraIncome,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.financial_detail_add_extra_income),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onEditIncome,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.financial_detail_edit_main_income),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodExtraExpensesDetailCard(
    uiState: FinancialDetailUiState,
    onEditExpense: (com.farbalapps.rinde.domain.model.ExtraExpense) -> Unit = {},
    onAddExpense: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Gastos del Hogar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = String.format(Locale.getDefault(), "$%,.0f MXN", uiState.extraExpensesTotal),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFEF5350)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.extraExpenses.isEmpty()) {
                Text(
                    text = "No se registraron gastos extra durante este periodo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dateFmt = java.text.SimpleDateFormat("dd MMM", Locale.getDefault())
                    uiState.extraExpenses.forEach { expense ->
                        val expenseDateMillis = if (expense.expenseDate > 0L) expense.expenseDate else expense.createdAt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEditExpense(expense) }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = expense.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = dateFmt.format(java.util.Date(expenseDateMillis)),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "- $${String.format(Locale.getDefault(), "%,.0f", expense.amount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFEF5350)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAddExpense,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.financial_detail_add_expense_empty),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun PeriodShoppingListDetailCard(
    uiState: FinancialDetailUiState,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Lista de Compras",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = String.format(Locale.getDefault(), "$%,.0f MXN", uiState.listTotal),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFEF5350)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.shoppingItems.isEmpty()) {
                Text(
                    text = "No tienes artículos marcados como comprados en tu lista de compras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "${uiState.shoppingItems.size} artículos comprados registrados en tu lista (${uiState.selectedPeriod.durationDays} días proporcional).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val previewItems = uiState.shoppingItems.take(5)
                    previewItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (item.emoji.isNotBlank()) "${item.emoji} " else ""}${item.name} (${String.format(Locale.getDefault(), if (item.quantity % 1.0 == 0.0) "%.0f" else "%.1f", item.quantity)} ${item.unit})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            val itemCost = (item.price ?: 0.0) * item.quantity
                            if (itemCost > 0.0) {
                                Text(
                                    text = String.format(Locale.getDefault(), "$%,.0f", itemCost),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (uiState.shoppingItems.size > 5) {
                        Text(
                            text = "+ ${uiState.shoppingItems.size - 5} artículos más en tu lista",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ir a Lista de compras",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PeriodGoalsDetailCard(
    uiState: FinancialDetailUiState,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color(0xFF26A69A),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Ahorro en Metas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = String.format(Locale.getDefault(), "$%,.0f MXN", uiState.goalsCommittedTotal),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF26A69A)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.activeGoals.isEmpty()) {
                Text(
                    text = "No tienes metas de ahorro activas para este periodo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "${uiState.activeGoals.size} metas activas en progreso protegidas durante este periodo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.activeGoals.forEach { goal ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "$%,.0f / $%,.0f", goal.currentAmount, goal.targetAmount),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF26A69A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ir a Metas de ahorro",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF26A69A)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF26A69A),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDetailContent(
    uiState: FinancialDetailUiState,
    onBack: () -> Unit,
    onNavigateToGoals: () -> Unit = {},
    onNavigateToList: () -> Unit = {},
    onPeriodTypeSelected: (PeriodType) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenFinancialCalendar: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onEditExpense: (com.farbalapps.rinde.domain.model.ExtraExpense) -> Unit,
    onEditIncome: () -> Unit = {},
    onDeleteSalaryClick: () -> Unit = {},
    onAddExtraIncome: () -> Unit = {},
    onEditExtraIncome: (ExtraIncome) -> Unit = {},
    onDeleteExtraIncome: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalle Financiero",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Selector de tipo de temporalidad (Mes, Quincena, Semana, Día, Personalizado)
            PeriodTypeSelectorRow(
                currentType = uiState.periodType,
                onSelectType = onPeriodTypeSelected
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Navegador del periodo seleccionado (< Fecha > 📅)
            PeriodNavigatorBar(
                displayName = uiState.selectedPeriod.displayName,
                canNavigateNext = uiState.canNavigateNext,
                onPrevious = onPreviousPeriod,
                onNext = onNextPeriod,
                onOpenCalendar = onOpenCalendar
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Card Hero con el estado de salud del periodo y acceso a Calendario
            PeriodSummaryHeroCard(
                uiState = uiState,
                onOpenCalendar = onOpenFinancialCalendar
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Detalle: Ingreso proporcional del periodo
            PeriodIncomeDetailCard(
                uiState = uiState,
                onEditIncome = onEditIncome,
                onDeleteSalaryClick = onDeleteSalaryClick,
                onAddExtraIncome = onAddExtraIncome,
                onEditExtraIncome = onEditExtraIncome,
                onDeleteExtraIncome = onDeleteExtraIncome
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Detalle: Gastos Extra del hogar
            PeriodExtraExpensesDetailCard(
                uiState = uiState,
                onEditExpense = onEditExpense,
                onAddExpense = onAddExpense
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Detalle: Lista de Compras
            PeriodShoppingListDetailCard(
                uiState = uiState,
                onClick = onNavigateToList
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Detalle: Ahorro en Metas
            PeriodGoalsDetailCard(
                uiState = uiState,
                onClick = onNavigateToGoals
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(name = "Financial Detail Light", showBackground = true)
@Composable
fun FinancialDetailContentPreview() {
    RindeTheme {
        FinancialDetailContent(
            uiState = FinancialDetailUiState(
                periodIncome = 15000.0,
                listTotal = 3200.0,
                extraExpensesTotal = 1500.0,
                goalsCommittedTotal = 2000.0
            ),
            onBack = {},
            onNavigateToGoals = {},
            onNavigateToList = {},
            onPeriodTypeSelected = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onOpenCalendar = {},
            onOpenFinancialCalendar = {},
            onAddExpense = {},
            onEditExpense = {},
            onEditIncome = {},
            onDeleteSalaryClick = {},
            onAddExtraIncome = {},
            onEditExtraIncome = {},
            onDeleteExtraIncome = {}
        )
    }
}

@Composable
private fun DeleteSalaryOptionItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
