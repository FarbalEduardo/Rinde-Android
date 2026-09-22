package com.farbalapps.rinde.ui.screen.home.list.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SaveListDialog(
    purchasedItemsCount: Int,
    unpurchasedItemsCount: Int,
    totalPrice: Double?,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, clearAfterSave: Boolean) -> Unit
) {
    val defaultDateStr = remember {
        val sdf = SimpleDateFormat("dd 'de' MMM, yyyy", Locale("es", "MX"))
        "Mandado del ${sdf.format(Date())}"
    }

    var listName by remember { mutableStateOf(defaultDateStr) }
    var clearAfterSave by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Guardar Lista Actual",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Crea un respaldo con los productos comprados de tu lista actual para consultarlos o usarlos en Chef IA.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text("Nombre de la lista") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Aviso de productos no comprados o lista vacía
                if (purchasedItemsCount == 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "No tienes productos marcados como comprados. Marca al menos un producto con la casilla para poder guardarlo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else if (unpurchasedItemsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Hay $unpurchasedItemsCount producto${if (unpurchasedItemsCount != 1) "s" else ""} pendiente${if (unpurchasedItemsCount != 1) "s" else ""} de compra que no se guardar${if (unpurchasedItemsCount != 1) "án" else "á"}. Solo se respaldarán los $purchasedItemsCount producto${if (purchasedItemsCount != 1) "s" else ""} marcados como comprados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                SaveListSummaryBlock(
                    purchasedItemsCount = purchasedItemsCount,
                    totalPrice = totalPrice,
                    currency = currency
                )

                if (purchasedItemsCount > 0) {
                    SaveListClearSwitchBlock(
                        clearAfterSave = clearAfterSave,
                        onClearAfterSaveChange = { clearAfterSave = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (listName.isNotBlank() && purchasedItemsCount > 0) {
                        onConfirm(listName.trim(), clearAfterSave)
                    }
                },
                enabled = listName.isNotBlank() && purchasedItemsCount > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SaveListSummaryBlock(
    purchasedItemsCount: Int,
    totalPrice: Double?,
    currency: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📦 $purchasedItemsCount producto${if (purchasedItemsCount != 1) "s" else ""} a guardar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            totalPrice?.let { price ->
                Text(
                    text = String.format(Locale.getDefault(), "Total: $%.2f %s", price, currency),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SaveListClearSwitchBlock(
    clearAfterSave: Boolean,
    onClearAfterSaveChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vaciar productos comprados",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Elimina de la pantalla solo los productos comprados guardados",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = clearAfterSave,
                onCheckedChange = onClearAfterSaveChange
            )
        }
    }
}
