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
    onConfirm: (title: String, targetAmount: Double, iconKey: String, colorKey: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    
    val availableIcons = GoalThemeMapper.getAvailableIcons()
    val availableColors = GoalThemeMapper.getAvailableColors()
    
    var selectedIconKey by remember { mutableStateOf(availableIcons.first().first) }
    var selectedColorKey by remember { mutableStateOf(availableColors.first().first) }
    
    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        dragHandle = { BottomSheetDefaults.DragHandle() }
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

            // Campo de Título
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 30) { // Regla E2.8 límite 30
                        title = it
                        titleError = null
                    }
                },
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

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de Monto
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    // Permitir sólo números y un decimal (Regla E2.7)
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = it
                        amountError = null
                    }
                },
                label = { Text("Monto Objetivo") },
                placeholder = { Text("0.00") },
                prefix = { Text("$ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = amountError != null,
                supportingText = { amountError?.let { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Ícono
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
                            .clickable { selectedIconKey = key },
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

            Spacer(modifier = Modifier.height(24.dp))

            // Selector de Color
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
                            .clickable { selectedColorKey = key },
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

            Spacer(modifier = Modifier.height(32.dp))

            // Confirmar botón
            Button(
                onClick = {
                    var hasError = false
                    if (title.isBlank()) {
                        titleError = "El nombre es requerido" // Regla E2.4
                        hasError = true
                    }
                    val targetVal = amountText.toDoubleOrNull() ?: 0.0
                    if (targetVal <= 0) {
                        amountError = "El monto debe ser mayor a cero" // Regla E2.5, E2.6
                        hasError = true
                    }
                    if (!hasError) {
                        onConfirm(title, targetVal, selectedIconKey, selectedColorKey)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) {
                Text(text = "Confirmar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
