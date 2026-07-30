package com.farbalapps.rinde.ui.screen.home.list.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling. preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.ShoppingItem as DomainShoppingItem
import com.farbalapps.rinde.ui.screen.home.list.toProductCategory
import com.farbalapps.rinde.ui.theme.RindeTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ShoppingListItem(
    item: DomainShoppingItem,
    isHighlighted: Boolean = false,
    isSwiped: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSwipeStateChange: (Boolean) -> Unit = {},
    onCheckedChange: (Boolean) -> Unit,
    onLongClick: () -> Unit = {},
    onSelectionToggle: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onIncrement: () -> Unit = {},
    onDecrement: () -> Unit = {},
    onQuantitySet: (Double) -> Unit = {},
    onSetPrice: (price: Double?, currency: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiCategory = item.category.toProductCategory()
    val haptic = LocalHapticFeedback.current
    var showQuickPriceDialog by remember { mutableStateOf(false) }
    var showQuantityDialog by remember { mutableStateOf(false) }

    // Material 3 Elevation & Color based on state
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface,
        label = "color"
    )

    // Borde Azul Animado (Resaltado Premium sin sombras parpadeantes)
    val borderAlpha by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "borderAlpha"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isHighlighted) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 600),
        label = "borderWidth"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        border = if (borderWidth > 0.dp) {
            BorderStroke(width = borderWidth, color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onSelectionToggle()
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LADO IZQUIERDO: Imagen/Emoji del producto
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (item.emoji.isNotEmpty()) {
                        Text(item.emoji, fontSize = 40.sp)
                    } else {
                        Icon(
                            imageVector = uiCategory.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // DERECHA: Contenido y Acciones
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // FILA SUPERIOR: Título, Categoría y Botones (X, Check)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Título y Categoría
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        val categoryText = if (uiCategory == com.farbalapps.rinde.ui.screen.home.list.ProductCategory.OTHERS) {
                            item.category
                        } else {
                            stringResource(id = uiCategory.displayNameRes)
                        }
                        Text(
                            text = categoryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Acciones superiores (Tachita y Checkbox)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!isSelectionMode) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Quitar producto",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onSelectionToggle() },
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = onCheckedChange,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FILA INFERIOR: Precio, Botón Editar, Botón Precio, y Cápsula de Cantidad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SECCIÓN IZQUIERDA: Precio e Íconos
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Si hay precio, mostrar el texto. Si no, mostrar el ícono de agregar precio.
                        if (item.price != null && item.price > 0.0) {
                            Text(
                                text = String.format(Locale.getDefault(), "$%.2f", item.price * item.quantity),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clickable { showQuickPriceDialog = true }
                                    .weight(1f, fill = false)
                            )
                        } else if (!isSelectionMode) {
                            // Icono de Agregar Precio
                            IconButton(
                                onClick = { showQuickPriceDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RequestQuote,
                                    contentDescription = "Agregar Precio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        if (!isSelectionMode) {
                            // Icono de Editar Producto
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editar Producto",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // SECCIÓN DERECHA: Cápsula de Cantidad (+ / -)
                    if (!isSelectionMode) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                // Botón (-) Menos
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    onClick = onDecrement,
                                    modifier = Modifier.size(28.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Disminuir",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Texto de Cantidad y Unidad
                                val quantityText = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                val shortUnit = when (item.unit.lowercase(Locale.getDefault())) {
                                    "piezas", "pieza" -> "pza"
                                    "kilogramos", "kilogramo", "kilos", "kilo" -> "kg"
                                    "gramos", "gramo" -> "g"
                                    "litros", "litro" -> "L"
                                    "mililitros", "mililitro" -> "ml"
                                    "paquetes", "paquete" -> "paq"
                                    "cajas", "caja" -> "cj"
                                    "botellas", "botella" -> "bot"
                                    "latas", "lata" -> "lat"
                                    else -> item.unit
                                }
                                Text(
                                    text = "$quantityText $shortUnit",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .clickable { showQuantityDialog = true }
                                        .padding(horizontal = 12.dp)
                                        .widthIn(max = 100.dp)
                                )

                                // Botón (+) Más
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    onClick = onIncrement,
                                    modifier = Modifier.size(28.dp),
                                    shadowElevation = 1.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Aumentar",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickPriceDialog) {
        QuickPriceDialog(
            itemName = item.name,
            initialPrice = item.price,
            onDismiss = { showQuickPriceDialog = false },
            onConfirm = { price ->
                onSetPrice(price, "MXN")
                showQuickPriceDialog = false
            }
        )
    }

    if (showQuantityDialog) {
        QuickQuantityDialog(
            itemName = item.name,
            initialQuantity = item.quantity,
            unit = item.unit,
            onDismiss = { showQuantityDialog = false },
            onConfirm = { newQty ->
                onQuantitySet(newQty)
                showQuantityDialog = false
            }
        )
    }
}

@Composable
fun QuickQuantityDialog(
    itemName: String,
    initialQuantity: Double,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double) -> Unit
) {
    var quantityText by remember {
        mutableStateOf(if (initialQuantity % 1.0 == 0.0) initialQuantity.toInt().toString() else initialQuantity.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cantidad para \"$itemName\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Cantidad ($unit)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = quantityText.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        onConfirm(parsed)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPriceDialog(
    itemName: String,
    initialPrice: Double?,
    onDismiss: () -> Unit,
    onConfirm: (price: Double?) -> Unit
) {
    var priceText by remember {
        mutableStateOf(initialPrice?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Precio para \"$itemName\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Precio unitario (MXN)") },
                prefix = { Text("$ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = priceText.toDoubleOrNull()
                    onConfirm(parsed)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row {
                if (initialPrice != null) {
                    TextButton(
                        onClick = { onConfirm(null) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Quitar")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@PreviewLightDark
@Composable
fun ShoppingListItemPreview() {
    val sampleItem = DomainShoppingItem(
        id = "1",
        name = "Limón fresco",
        category = "Frutas",
        quantity = 2.0,
        unit = "kg",
        isCompleted = false,
        emoji = "🍋",
        price = 0.0
    )
    RindeTheme {
        ShoppingListItem(
            item = sampleItem,
            isSwiped = false,
            onSwipeStateChange = {},
            onCheckedChange = {},
            onEdit = {},
            onDelete = {}
        )
    }
}
