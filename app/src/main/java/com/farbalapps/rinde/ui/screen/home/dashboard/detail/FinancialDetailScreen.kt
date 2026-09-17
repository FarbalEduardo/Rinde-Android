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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    modifier: Modifier = Modifier,
    viewModel: FinancialDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FinancialDetailContent(
        uiState = uiState,
        onBack = onBack,
        onPeriodTypeSelected = { viewModel.setPeriodType(it) },
        onPreviousPeriod = { viewModel.navigatePrevious() },
        onNextPeriod = { viewModel.navigateNext() },
        onOpenCalendar = { viewModel.openDatePicker() },
        onEditExpense = { viewModel.openEditExpense(it) },
        modifier = modifier
    )

    // Modal selector de un solo día
    if (uiState.showDatePickerModal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedPeriod.startTimestamp
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
            initialSelectedStartDateMillis = uiState.selectedPeriod.startTimestamp,
            initialSelectedEndDateMillis = uiState.selectedPeriod.endTimestamp
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

    // Modal para editar o eliminar un gasto extra del hogar
    val currentEditingExpense = uiState.editingExpense
    if (uiState.isEditExpenseSheetOpen && currentEditingExpense != null) {
        com.farbalapps.rinde.ui.screen.home.dashboard.components.ExtraExpenseEditBottomSheet(
            expense = currentEditingExpense,
            onDismiss = { viewModel.closeEditExpense() },
            onUpdate = { id, label, amount -> viewModel.updateExtraExpense(id, label, amount) },
            onDelete = { id -> viewModel.deleteExtraExpense(id) }
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

            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PeriodSummaryHeroCard(
    uiState: FinancialDetailUiState
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
            Text(
                text = availableFormatted,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp
                ),
                color = if (uiState.availableAmount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "${uiState.currency} · Disponible neto para este periodo (${uiState.selectedPeriod.durationDays} días)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )

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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Comparativa de 3 columnas (Ingreso, Gastos, Balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem(
                    label = "Ingreso periodo",
                    amount = String.format(Locale.getDefault(), "$%,.0f", uiState.periodIncome),
                    color = Color(0xFF66BB6A)
                )
                SummaryMetricItem(
                    label = "Gastos totales",
                    amount = String.format(Locale.getDefault(), "$%,.0f", uiState.totalExpenses),
                    color = Color(0xFFEF5350)
                )
                SummaryMetricItem(
                    label = "Disponible",
                    amount = availableFormatted,
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
    color: Color
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
    }
}

@Composable
private fun PeriodIncomeDetailCard(
    uiState: FinancialDetailUiState
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

            Spacer(modifier = Modifier.height(10.dp))

            val formattedIncome = String.format(Locale.getDefault(), "$%,.0f MXN", uiState.periodIncome)
            Text(
                text = formattedIncome,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF66BB6A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            val freqText = when (uiState.profile?.incomeFrequency) {
                com.farbalapps.rinde.domain.model.IncomeFrequency.MONTHLY -> "Mensual"
                com.farbalapps.rinde.domain.model.IncomeFrequency.BIWEEKLY -> "Quincenal"
                com.farbalapps.rinde.domain.model.IncomeFrequency.WEEKLY -> "Semanal"
                com.farbalapps.rinde.domain.model.IncomeFrequency.DAILY -> "Diario"
                com.farbalapps.rinde.domain.model.IncomeFrequency.CUSTOM -> "Personalizado"
                null -> "No configurado"
            }
            Text(
                text = "Periodicidad configurada: $freqText ($${String.format(Locale.getDefault(), "%,.0f", uiState.profile?.income ?: 0.0)}). Tasa diaria calculada: $${String.format(Locale.getDefault(), "%,.0f", uiState.profile?.dailyIncome ?: 0.0)}/día.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeriodExtraExpensesDetailCard(
    uiState: FinancialDetailUiState,
    onEditExpense: (com.farbalapps.rinde.domain.model.ExtraExpense) -> Unit = {}
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
                                    text = dateFmt.format(java.util.Date(expense.createdAt)),
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
        }
    }
}

@Composable
private fun PeriodShoppingListDetailCard(
    uiState: FinancialDetailUiState
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

            Text(
                text = "${uiState.shoppingItems.size} artículos registrados en tu lista de compras actual (${uiState.selectedPeriod.durationDays} días proporcional).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.shoppingItems.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No tienes artículos agregados en tu lista de compras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
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
        }
    }
}

@Composable
private fun PeriodGoalsDetailCard(
    uiState: FinancialDetailUiState
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

            Text(
                text = "${uiState.activeGoals.size} metas activas en progreso protegidas durante este periodo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.activeGoals.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No tienes metas de ahorro activas para este periodo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
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
        }
    }
}

@Composable
private fun FormulaBreakdownCard(
    uiState: FinancialDetailUiState
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "📐 Balance del Periodo:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ingreso ($${String.format(Locale.getDefault(), "%,.0f", uiState.periodIncome)}) - Gastos Hogar ($${String.format(Locale.getDefault(), "%,.0f", uiState.extraExpensesTotal)}) - Lista ($${String.format(Locale.getDefault(), "%,.0f", uiState.listTotal)}) - Metas ($${String.format(Locale.getDefault(), "%,.0f", uiState.goalsCommittedTotal)}) = $${String.format(Locale.getDefault(), "%,.0f", uiState.availableAmount)} Disponible.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialDetailContent(
    uiState: FinancialDetailUiState,
    onBack: () -> Unit,
    onPeriodTypeSelected: (PeriodType) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onOpenCalendar: () -> Unit,
    onEditExpense: (com.farbalapps.rinde.domain.model.ExtraExpense) -> Unit,
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
                onPrevious = onPreviousPeriod,
                onNext = onNextPeriod,
                onOpenCalendar = onOpenCalendar
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Card Hero con el estado de salud del periodo
            PeriodSummaryHeroCard(
                uiState = uiState
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Detalle: Ingreso proporcional del periodo
            PeriodIncomeDetailCard(
                uiState = uiState
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Detalle: Gastos Extra del hogar
            PeriodExtraExpensesDetailCard(
                uiState = uiState,
                onEditExpense = onEditExpense
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Detalle: Lista de Compras
            PeriodShoppingListDetailCard(
                uiState = uiState
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Detalle: Ahorro en Metas
            PeriodGoalsDetailCard(
                uiState = uiState
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 8. Resumen explicativo de la fórmula
            FormulaBreakdownCard(
                uiState = uiState
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
            onPeriodTypeSelected = {},
            onPreviousPeriod = {},
            onNextPeriod = {},
            onOpenCalendar = {},
            onEditExpense = {}
        )
    }
}
