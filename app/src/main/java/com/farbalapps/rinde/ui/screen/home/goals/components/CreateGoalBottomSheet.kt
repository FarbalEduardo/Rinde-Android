package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, iconKey: String, colorKey: String, startDate: Long, targetDate: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    
    val availableIcons = GoalThemeMapper.getAvailableIcons()
    val availableColors = GoalThemeMapper.getAvailableColors()
    
    var selectedIconKey by remember { mutableStateOf(availableIcons.first().first) }
    var selectedColorKey by remember { mutableStateOf(availableColors.first().first) }
    
    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var targetDate by remember { mutableStateOf(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) } // +30 días por defecto

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Nueva Meta de Ahorro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            TitleInputField(title, titleError) { newTitle ->
                if (newTitle.length <= 30) {
                    title = newTitle
                    titleError = null
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AmountInputField(amountText, amountError) { newAmount ->
                if (newAmount.isEmpty() || newAmount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    amountText = newAmount
                    amountError = null
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    DateField(label = "Fecha Inicio", dateMillis = startDate, onDateSelected = { startDate = it })
                }
                Box(modifier = Modifier.weight(1f)) {
                    DateField(label = "Fecha Límite", dateMillis = targetDate, onDateSelected = { targetDate = it })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            IconSelector(availableIcons, selectedIconKey) { selectedIconKey = it }

            Spacer(modifier = Modifier.height(24.dp))

            ColorSelector(availableColors, selectedColorKey) { selectedColorKey = it }

            Spacer(modifier = Modifier.height(32.dp))

            GoalConfirmButton(
                title = title,
                amountText = amountText,
                selectedIconKey = selectedIconKey,
                selectedColorKey = selectedColorKey,
                startDate = startDate,
                targetDate = targetDate,
                onConfirm = onConfirm,
                onTitleError = { titleError = it },
                onAmountError = { amountError = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    dateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val formattedDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(dateMillis))

    Box(modifier = Modifier.fillMaxWidth().clickable { showDialog = true }) {
        OutlinedTextField(
            value = formattedDate,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }

    if (showDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun TitleInputField(
    title: String,
    titleError: String?,
    onTitleChange: (String) -> Unit
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("¿Para qué estás ahorrando?") },
        placeholder = { Text("Ej. Viaje a Japón") },
        isError = titleError != null,
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = titleError ?: "", color = MaterialTheme.colorScheme.error)
                Text(text = "${title.length}/30")
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AmountInputField(
    amountText: String,
    amountError: String?,
    onAmountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = amountText,
        onValueChange = onAmountChange,
        label = { Text("Monto Objetivo") },
        placeholder = { Text("0.00") },
        prefix = { Text("$ ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = amountError != null,
        supportingText = { amountError?.let { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun IconSelector(
    availableIcons: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    selectedIconKey: String,
    onIconSelected: (String) -> Unit
) {
    Text(
        text = "Selecciona un ícono",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(availableIcons) { (key, vector) ->
            val isSelected = selectedIconKey == key
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { onIconSelected(key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vector,
                    contentDescription = key,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorSelector(
    availableColors: List<Pair<String, Color>>,
    selectedColorKey: String,
    onColorSelected: (String) -> Unit
) {
    Text(
        text = "Selecciona un color temático",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(availableColors) { (key, color) ->
            val isSelected = selectedColorKey == key
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = color, shape = CircleShape)
                    .clip(CircleShape)
                    .clickable { onColorSelected(key) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.White, shape = CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalConfirmButton(
    title: String,
    amountText: String,
    selectedIconKey: String,
    selectedColorKey: String,
    startDate: Long,
    targetDate: Long,
    onConfirm: (String, Double, String, String, Long, Long) -> Unit,
    onTitleError: (String) -> Unit,
    onAmountError: (String) -> Unit
) {
    Button(
        onClick = {
            var hasError = false
            if (title.isBlank()) {
                onTitleError("El nombre es requerido") // Regla E2.4
                hasError = true
            }
            val targetVal = amountText.toDoubleOrNull() ?: 0.0
            if (targetVal <= 0) {
                onAmountError("El monto debe ser mayor a cero") // Regla E2.5, E2.6
                hasError = true
            }
            if (!hasError) {
                onConfirm(title, targetVal, selectedIconKey, selectedColorKey, startDate, targetDate)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50)
    ) {
        Text(text = "Confirmar", fontWeight = FontWeight.Bold)
    }
}
