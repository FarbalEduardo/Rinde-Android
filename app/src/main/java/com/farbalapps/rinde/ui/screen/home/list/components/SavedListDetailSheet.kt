package com.farbalapps.rinde.ui.screen.home.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.domain.model.SavedShoppingList
import com.farbalapps.rinde.domain.usecase.LoadSavedListMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedListDetailSheet(
    savedList: SavedShoppingList,
    availableGroups: List<String>,
    onDismiss: () -> Unit,
    onRenameClick: () -> Unit,
    onLoadList: (savedList: SavedShoppingList, mode: LoadSavedListMode, group: String) -> Unit
) {
    var showGroupPicker by remember { mutableStateOf(false) }
    var selectedGroupForAssign by remember { mutableStateOf(availableGroups.firstOrNull { it != "All" } ?: "General") }

    val dateFormatted = remember(savedList.savedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "MX"))
        sdf.format(Date(savedList.savedAt))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = savedList.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onRenameClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Renombrar lista",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Guardada el $dateFormatted · ${savedList.totalItems} productos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                savedList.totalPrice?.let { price ->
                    Text(
                        text = String.format(Locale.getDefault(), "$%.2f %s", price, savedList.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Items list read-only preview
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(savedList.items, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (item.emoji.isNotEmpty()) item.emoji else "🛒",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${item.quantity} ${item.unit} · ${item.category}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            item.price?.let { price ->
                                Text(
                                    text = String.format(Locale.getDefault(), "$%.2f", price),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()

            // 4 Load Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "¿Qué deseas hacer con esta lista?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        onLoadList(savedList, LoadSavedListMode.MERGE_CURRENT, "All")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar a lista actual (fusionar)")
                }

                if (showGroupPicker) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterGroups = availableGroups.filter { it != "All" }
                        if (filterGroups.isNotEmpty()) {
                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Grupo: $selectedGroupForAssign")
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    filterGroups.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group) },
                                            onClick = {
                                                selectedGroupForAssign = group
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {
                                onLoadList(savedList, LoadSavedListMode.ASSIGN_GROUP, selectedGroupForAssign)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirmar")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showGroupPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar a una lista/grupo específico")
                    }
                }

                OutlinedButton(
                    onClick = {
                        onLoadList(savedList, LoadSavedListMode.REPLACE_NEW, savedList.name)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir en una lista nueva (\"${savedList.name}\")")
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Solo consultar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
