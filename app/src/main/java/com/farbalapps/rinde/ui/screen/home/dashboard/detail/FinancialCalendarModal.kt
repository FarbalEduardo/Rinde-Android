package com.farbalapps.rinde.ui.screen.home.dashboard.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.ExtraIncome
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.IncomeFrequency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Modelo para representar un movimiento unificado en el calendario financiero.
 */
data class CalendarMovementItem(
    val id: String,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val dateMillis: Long,
    val subtitle: String = "",
    val isSalary: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCalendarModal(
    initialYear: Int,
    initialMonth: Int, // 1..12
    extraExpenses: List<ExtraExpense>,
    extraIncomes: List<ExtraIncome>,
    profile: FinancialProfile?,
    currency: String = "MXN",
    onDismiss: () -> Unit,
    onDeleteSalarySingleMonth: (Int, Int) -> Unit = { _, _ -> },
    onDeleteSalaryFutureMonths: (Int, Int) -> Unit = { _, _ -> },
    onDeleteSalaryAll: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentYear by remember { mutableIntStateOf(initialYear) }
    var currentMonth by remember { mutableIntStateOf(initialMonth) }
    var suppressedMonths by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    var showDeleteSalaryDialog by remember { mutableStateOf(false) }
    var selectedDeleteSalaryOption by remember { mutableIntStateOf(1) }

    // Día seleccionado (1..numDays)
    val todayCal = remember { Calendar.getInstance() }
    val initialSelectedDay = remember(currentYear, currentMonth) {
        if (todayCal.get(Calendar.YEAR) == currentYear && todayCal.get(Calendar.MONTH) + 1 == currentMonth) {
            todayCal.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }
    }
    var selectedDay by remember { mutableIntStateOf(initialSelectedDay) }

    // Calcular días del mes y desplazamiento inicial (Lunes = 0 .. Domingo = 6)
    val calendarInfo = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, Tuesday=3, ... Saturday=7
        val javaDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert to Monday=0 .. Sunday=6
        val firstDayOffset = (javaDayOfWeek + 5) % 7
        Triple(daysInMonth, firstDayOffset, cal.timeInMillis)
    }
    val daysInMonth = calendarInfo.first
    val firstDayOffset = calendarInfo.second

    val monthName = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        sdf.format(cal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    // Mapear movimientos a días
    val movementsByDay = remember(currentYear, currentMonth, extraExpenses, extraIncomes, profile) {
        val map = mutableMapOf<Int, MutableList<CalendarMovementItem>>()
        val cal = Calendar.getInstance()

        // 1. Gastos extras del hogar
        extraExpenses.forEach { expense ->
            val expenseDate = if (expense.expenseDate > 0L) expense.expenseDate else expense.createdAt
            cal.timeInMillis = expenseDate
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) + 1 == currentMonth) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val list = map.getOrPut(day) { mutableListOf() }
                list.add(
                    CalendarMovementItem(
                        id = "exp_${expense.id}",
                        title = expense.label,
                        amount = expense.amount,
                        isIncome = false,
                        dateMillis = expenseDate,
                        subtitle = "Gasto del hogar"
                    )
                )
            }
        }

        // 2. Ganancias extras
        extraIncomes.forEach { income ->
            val incomeDate = if (income.incomeDate > 0L) income.incomeDate else income.createdAt
            cal.timeInMillis = incomeDate
            if (cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) + 1 == currentMonth) {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val list = map.getOrPut(day) { mutableListOf() }
                list.add(
                    CalendarMovementItem(
                        id = "inc_${income.id}",
                        title = income.label,
                        amount = income.amount,
                        isIncome = true,
                        dateMillis = incomeDate,
                        subtitle = "Ganancia extra"
                    )
                )
            }
        }

        // 3. Sueldo fijo o recurrente si tiene fecha ancla de cobro
        if (profile != null && profile.income > 0.0 && (currentYear to currentMonth) !in suppressedMonths) {
            val anchorDate = profile.customStartDate ?: profile.updatedAt
            val calAnchor = Calendar.getInstance().apply { timeInMillis = if (anchorDate > 0L) anchorDate else System.currentTimeMillis() }
            val anchorYear = calAnchor.get(Calendar.YEAR)
            val anchorMonth = calAnchor.get(Calendar.MONTH) + 1
            val anchorDay = calAnchor.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)

            if (profile.isVariableIncome) {
                // Ingreso Variable: Solo se muestra en el mes y año específico donde fue configurado
                if (currentYear == anchorYear && currentMonth == anchorMonth) {
                    val list = map.getOrPut(anchorDay) { mutableListOf() }
                    list.add(
                        CalendarMovementItem(
                            id = "variable_income",
                            title = "Ingreso Variable",
                            amount = profile.income,
                            isIncome = true,
                            dateMillis = calAnchor.timeInMillis,
                            subtitle = "Ingreso capturado para este mes",
                            isSalary = false
                        )
                    )
                }
            } else {
                // Sueldo fijo mensual / periódico
                // Validar que no aparezca en meses anteriores a su fecha de configuración/inicio
                val isBeforeStart = anchorDate > 0L && (currentYear < anchorYear || (currentYear == anchorYear && currentMonth < anchorMonth))

                // Validar que no aparezca después de customEndDate si existe
                val isAfterEnd = if (profile.customEndDate != null && profile.customEndDate > 0L) {
                    val endCal = Calendar.getInstance().apply { timeInMillis = profile.customEndDate }
                    val endYear = endCal.get(Calendar.YEAR)
                    val endMonth = endCal.get(Calendar.MONTH) + 1
                    currentYear > endYear || (currentYear == endYear && currentMonth > endMonth)
                } else {
                    false
                }

                if (!isBeforeStart && !isAfterEnd) {
                    when (profile.incomeFrequency) {
                        IncomeFrequency.MONTHLY -> {
                            val list = map.getOrPut(anchorDay) { mutableListOf() }
                            list.add(
                                CalendarMovementItem(
                                    id = "base_salary",
                                    title = "Sueldo Mensual Base",
                                    amount = profile.income,
                                    isIncome = true,
                                    dateMillis = calAnchor.timeInMillis,
                                    subtitle = "Ingreso base configurado",
                                    isSalary = true
                                )
                            )
                        }
                        IncomeFrequency.BIWEEKLY -> {
                            val day1 = anchorDay
                            val day2 = ((anchorDay + 14) % daysInMonth) + 1
                            val list1 = map.getOrPut(day1) { mutableListOf() }
                            list1.add(
                                CalendarMovementItem(
                                    id = "base_salary_bi1",
                                    title = "Pago Quincenal (1/2)",
                                    amount = profile.income,
                                    isIncome = true,
                                    dateMillis = calAnchor.timeInMillis,
                                    subtitle = "Ingreso quincenal",
                                    isSalary = true
                                )
                            )
                            val list2 = map.getOrPut(day2) { mutableListOf() }
                            list2.add(
                                CalendarMovementItem(
                                    id = "base_salary_bi2",
                                    title = "Pago Quincenal (2/2)",
                                    amount = profile.income,
                                    isIncome = true,
                                    dateMillis = calAnchor.timeInMillis,
                                    subtitle = "Ingreso quincenal",
                                    isSalary = true
                                )
                            )
                        }
                        IncomeFrequency.WEEKLY -> {
                            val anchorDayOfWeek = calAnchor.get(Calendar.DAY_OF_WEEK)
                            for (d in 1..daysInMonth) {
                                cal.set(Calendar.YEAR, currentYear)
                                cal.set(Calendar.MONTH, currentMonth - 1)
                                cal.set(Calendar.DAY_OF_MONTH, d)
                                if (cal.get(Calendar.DAY_OF_WEEK) == anchorDayOfWeek) {
                                    val list = map.getOrPut(d) { mutableListOf() }
                                    list.add(
                                        CalendarMovementItem(
                                            id = "base_salary_week_$d",
                                            title = "Sueldo Semanal",
                                            amount = profile.income,
                                            isIncome = true,
                                            dateMillis = cal.timeInMillis,
                                            subtitle = "Ingreso semanal",
                                            isSalary = true
                                        )
                                    )
                                }
                            }
                        }
                        IncomeFrequency.DAILY -> {
                        }
                        IncomeFrequency.CUSTOM -> {
                            val list = map.getOrPut(anchorDay) { mutableListOf() }
                            list.add(
                                CalendarMovementItem(
                                    id = "base_salary_custom",
                                    title = "Ingreso Periodo",
                                    amount = profile.income,
                                    isIncome = true,
                                    dateMillis = calAnchor.timeInMillis,
                                    subtitle = "Ingreso personalizado",
                                    isSalary = true
                                )
                            )
                        }
                    }
                }
            }
        }

        map
    }

    val selectedDayMovements = movementsByDay[selectedDay] ?: emptyList()
    val selectedDayIncomes = selectedDayMovements.filter { it.isIncome }.sumOf { it.amount }
    val selectedDayExpenses = selectedDayMovements.filter { !it.isIncome }.sumOf { it.amount }
    val selectedDayBalance = selectedDayIncomes - selectedDayExpenses

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Cabecera: Título y Navegación de Mes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.financial_detail_calendar_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Flechas de navegación de mes
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            if (currentMonth == 1) {
                                currentMonth = 12
                                currentYear -= 1
                            } else {
                                currentMonth -= 1
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Mes anterior",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            if (currentMonth == 12) {
                                currentMonth = 1
                                currentYear += 1
                            } else {
                                currentMonth += 1
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Mes siguiente",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Leyenda Visual: 🟢 Ingreso | 🔴 Gasto o Pago
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ingresos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gastos / Pagos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rejilla del Calendario (Días de la semana)
            val weekDayLabels = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Celdas del Calendario (Semanas)
            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - firstDayOffset + 1

                            if (dayNumber in 1..daysInMonth) {
                                val isSelected = (dayNumber == selectedDay)
                                val dayMovements = movementsByDay[dayNumber] ?: emptyList()
                                val hasIncome = dayMovements.any { it.isIncome }
                                val hasExpense = dayMovements.any { !it.isIncome }

                                val isToday = (todayCal.get(Calendar.YEAR) == currentYear &&
                                        todayCal.get(Calendar.MONTH) + 1 == currentMonth &&
                                        todayCal.get(Calendar.DAY_OF_MONTH) == dayNumber)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                            else if (isToday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else if (isToday) 1.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDay = dayNumber },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )

                                        // Puntos indicadores de movimientos
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (hasIncome) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF2E7D32))
                                                )
                                            }
                                            if (hasExpense) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFD32F2F))
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Detalle del Día Seleccionado
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Encabezado del día y balance neto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Día $selectedDay de $monthName",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (selectedDayMovements.isNotEmpty()) {
                            val balanceSign = if (selectedDayBalance >= 0) "+" else ""
                            val balanceColor = if (selectedDayBalance >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            Text(
                                text = "$balanceSign$${String.format(Locale.getDefault(), "%,.0f", selectedDayBalance)} $currency",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = balanceColor
                            )
                        }
                    }

                    if (selectedDayMovements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (selectedDayIncomes > 0) {
                                Text(
                                    text = "Ingresos: +$${String.format(Locale.getDefault(), "%,.0f", selectedDayIncomes)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            if (selectedDayExpenses > 0) {
                                Text(
                                    text = "Gastos: -$${String.format(Locale.getDefault(), "%,.0f", selectedDayExpenses)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Lista de Movimientos del Día
                    if (selectedDayMovements.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.financial_calendar_no_events),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedDayMovements, key = { it.id }) { item ->
                                CalendarMovementRow(
                                    item = item,
                                    currency = currency,
                                    onDeleteSalaryClick = if (item.isSalary) {
                                        { showDeleteSalaryDialog = true }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteSalaryDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSalaryDialog = false },
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
                        isSelected = selectedDeleteSalaryOption == 1,
                        onClick = { selectedDeleteSalaryOption = 1 }
                    )

                    DeleteSalaryOptionItem(
                        title = stringResource(id = R.string.financial_calendar_opt_this_and_future),
                        description = stringResource(id = R.string.financial_calendar_opt_this_and_future_desc),
                        isSelected = selectedDeleteSalaryOption == 2,
                        onClick = { selectedDeleteSalaryOption = 2 }
                    )

                    DeleteSalaryOptionItem(
                        title = stringResource(id = R.string.financial_calendar_opt_all),
                        description = stringResource(id = R.string.financial_calendar_opt_all_desc),
                        isSelected = selectedDeleteSalaryOption == 3,
                        onClick = { selectedDeleteSalaryOption = 3 }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (selectedDeleteSalaryOption) {
                            1 -> {
                                suppressedMonths = suppressedMonths + (currentYear to currentMonth)
                                onDeleteSalarySingleMonth(currentYear, currentMonth)
                            }
                            2 -> {
                                onDeleteSalaryFutureMonths(currentYear, currentMonth)
                            }
                            3 -> {
                                onDeleteSalaryAll()
                            }
                        }
                        showDeleteSalaryDialog = false
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
                TextButton(onClick = { showDeleteSalaryDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
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
        border = BorderStroke(
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

@Composable
private fun CalendarMovementRow(
    item: CalendarMovementItem,
    currency: String,
    modifier: Modifier = Modifier,
    onDeleteSalaryClick: (() -> Unit)? = null
) {
    val amountColor = if (item.isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F)
    val amountPrefix = if (item.isIncome) "+$" else "-$"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isIncome) Color(0xFF2E7D32).copy(alpha = 0.15f)
                        else Color(0xFFD32F2F).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isIncome) Icons.Outlined.Payments else Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = if (item.isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$amountPrefix${String.format(Locale.getDefault(), "%,.0f", item.amount)} $currency",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )

            if (item.isSalary && onDeleteSalaryClick != null) {
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
