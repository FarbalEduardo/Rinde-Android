package com.farbalapps.rinde.ui.screen.home.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.IncomeFrequency
import java.util.Locale

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeSetupBottomSheet(
    currentProfile: FinancialProfile?,
    currentMonthName: String = "",
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        frequency: IncomeFrequency,
        customStartDate: Long?,
        customEndDate: Long?,
        isVariable: Boolean,
        paymentDate: Long?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isVariableIncome by remember {
        mutableStateOf(currentProfile?.isVariableIncome ?: false)
    }

    var amountText by remember {
        mutableStateOf(
            if (currentProfile != null && currentProfile.income > 0) {
                if (currentProfile.income % 1.0 == 0.0) {
                    currentProfile.income.toInt().toString()
                } else {
                    currentProfile.income.toString()
                }
            } else ""
        )
    }

    var selectedFrequency by remember {
        mutableStateOf(currentProfile?.incomeFrequency ?: IncomeFrequency.MONTHLY)
    }

    var paymentDateMillis by remember {
        mutableStateOf(currentProfile?.customStartDate ?: System.currentTimeMillis())
    }

    var showPaymentDatePicker by remember { mutableStateOf(false) }

    var customStartDate by remember {
        mutableStateOf(currentProfile?.customStartDate ?: System.currentTimeMillis())
    }

    var customEndDate by remember {
        mutableStateOf(
            currentProfile?.customEndDate ?: (System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000)
        )
    }

    var showDateRangePicker by remember { mutableStateOf(false) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0

    val customDays = run {
        val diff = kotlin.math.max(0L, customEndDate - customStartDate)
        kotlin.math.max(1, (diff / (1000L * 60 * 60 * 24)).toInt() + 1)
    }

    val monthlyEquiv = when (selectedFrequency) {
        IncomeFrequency.MONTHLY -> parsedAmount
        IncomeFrequency.BIWEEKLY -> parsedAmount * 2.0
        IncomeFrequency.WEEKLY -> parsedAmount * (52.0 / 12.0)
        IncomeFrequency.DAILY -> parsedAmount * 30.0
        IncomeFrequency.CUSTOM -> (parsedAmount / customDays) * 30.0
    }

    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_income_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = R.string.dashboard_income_sheet_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Selector Fijo (Paulatino) vs Variable (Por mes)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    val fixedSelected = !isVariableIncome
                    val variableSelected = isVariableIncome

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (fixedSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isVariableIncome = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.dashboard_income_type_fixed),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (fixedSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (fixedSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (variableSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { isVariableIncome = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.dashboard_income_type_variable),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (variableSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (variableSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isVariableIncome) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.dashboard_income_variable_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Monto
            val labelString = if (isVariableIncome && currentMonthName.isNotBlank()) {
                stringResource(id = R.string.dashboard_income_variable_label, currentMonthName)
            } else {
                stringResource(id = R.string.dashboard_income_amount_label)
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) {
                        amountText = input
                    }
                },
                label = { Text(labelString) },
                prefix = {
                    Text(
                        text = "$ ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                suffix = {
                    Text(
                        text = "MXN",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Selector de Fecha de Cobro (para variable o recurrente)
            if (isVariableIncome || selectedFrequency != IncomeFrequency.CUSTOM) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedCard(
                    onClick = { showPaymentDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isVariableIncome) "Fecha de cobro de este mes" else "Día o fecha de cobro",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateFormat.format(Date(paymentDateMillis)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = stringResource(id = R.string.financial_select_date),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isVariableIncome) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Periodicidad del ingreso",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selector de frecuencia (Fila 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrequencyOptionCard(
                        title = stringResource(id = R.string.dashboard_freq_monthly),
                        subtitle = stringResource(id = R.string.dashboard_freq_monthly_sub),
                        isSelected = selectedFrequency == IncomeFrequency.MONTHLY,
                        onClick = { selectedFrequency = IncomeFrequency.MONTHLY },
                        modifier = Modifier.weight(1f)
                    )

                    FrequencyOptionCard(
                        title = stringResource(id = R.string.dashboard_freq_biweekly),
                        subtitle = stringResource(id = R.string.dashboard_freq_biweekly_sub),
                        isSelected = selectedFrequency == IncomeFrequency.BIWEEKLY,
                        onClick = { selectedFrequency = IncomeFrequency.BIWEEKLY },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selector de frecuencia (Fila 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FrequencyOptionCard(
                        title = stringResource(id = R.string.dashboard_freq_weekly),
                        subtitle = stringResource(id = R.string.dashboard_freq_weekly_sub),
                        isSelected = selectedFrequency == IncomeFrequency.WEEKLY,
                        onClick = { selectedFrequency = IncomeFrequency.WEEKLY },
                        modifier = Modifier.weight(1f)
                    )

                    FrequencyOptionCard(
                        title = stringResource(id = R.string.dashboard_freq_daily),
                        subtitle = stringResource(id = R.string.dashboard_freq_daily_sub),
                        isSelected = selectedFrequency == IncomeFrequency.DAILY,
                        onClick = { selectedFrequency = IncomeFrequency.DAILY },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Opción 5: Periodo Personalizado (con Calendario)
                FrequencyOptionCard(
                    title = "🗓️ Periodo personalizado",
                    subtitle = if (selectedFrequency == IncomeFrequency.CUSTOM) {
                        "${dateFormat.format(Date(customStartDate))} - ${dateFormat.format(Date(customEndDate))} ($customDays días)"
                    } else {
                        "Elegir fechas exactas en calendario"
                    },
                    isSelected = selectedFrequency == IncomeFrequency.CUSTOM,
                    onClick = {
                        selectedFrequency = IncomeFrequency.CUSTOM
                        showDateRangePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedFrequency == IncomeFrequency.CUSTOM) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDateRangePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cambiar rango de fechas en calendario 📅")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Nota de equivalencia mensual
                if (parsedAmount > 0) {
                    val formattedEquiv = String.format(Locale.getDefault(), "$%,.0f MXN", monthlyEquiv)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.dashboard_monthly_equiv_note, formattedEquiv),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Botón Guardar
            Button(
                onClick = {
                    if (parsedAmount > 0.0) {
                        onSave(
                            parsedAmount,
                            selectedFrequency,
                            if (selectedFrequency == IncomeFrequency.CUSTOM) customStartDate else paymentDateMillis,
                            if (selectedFrequency == IncomeFrequency.CUSTOM) customEndDate else null,
                            isVariableIncome,
                            if (isVariableIncome) paymentDateMillis else null
                        )
                    }
                },
                enabled = parsedAmount > 0.0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Guardar ingreso",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }

    // Modal de selección de fecha individual de cobro
    if (showPaymentDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = paymentDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showPaymentDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            paymentDateMillis = millis + 12 * 60 * 60 * 1000L
                        }
                        showPaymentDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Seleccionar fecha de cobro",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }
    }

    // Modal de selección de rango de fechas
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStartDate,
            initialSelectedEndDateMillis = customEndDate
        )
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateRangePickerState.selectedStartDateMillis?.let { start ->
                            customStartDate = start
                            customEndDate = dateRangePickerState.selectedEndDateMillis ?: start
                        }
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = "Selecciona el periodo del ingreso",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FrequencyOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
