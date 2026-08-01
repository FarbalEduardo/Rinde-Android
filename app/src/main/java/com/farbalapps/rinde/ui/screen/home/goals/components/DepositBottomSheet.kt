package com.farbalapps.rinde.ui.screen.home.goals.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositBottomSheet(
    goalTitle: String,
    onDismissRequest: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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
                text = "Ahorrar para \"$goalTitle\"",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            DepositAmountField(
                amountText = amountText,
                amountError = amountError,
                onAmountChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = it
                        amountError = null
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DepositNoteField(
                noteText = noteText,
                onNoteChange = { noteText = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            DepositConfirmButton(
                amountText = amountText,
                noteText = noteText,
                onConfirm = onConfirm,
                onAmountError = { amountError = it }
            )
        }
    }
}

@Composable
private fun DepositAmountField(
    amountText: String,
    amountError: String?,
    onAmountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = amountText,
        onValueChange = onAmountChange,
        label = { Text("Monto a depositar") },
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
private fun DepositNoteField(
    noteText: String,
    onNoteChange: (String) -> Unit
) {
    OutlinedTextField(
        value = noteText,
        onValueChange = onNoteChange,
        label = { Text("Nota o concepto (opcional)") },
        placeholder = { Text("Ej. Ahorro de la semana 🚀") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DepositConfirmButton(
    amountText: String,
    noteText: String,
    onConfirm: (Double, String) -> Unit,
    onAmountError: (String) -> Unit
) {
    Button(
        onClick = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                onAmountError("El monto debe ser mayor a cero") // Regla E3.2
            } else {
                onConfirm(amount, noteText)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50)
    ) {
        Text(text = "Confirmar Depósito", fontWeight = FontWeight.Bold)
    }
}
