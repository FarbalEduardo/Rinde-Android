package com.farbalapps.rinde.ui.screen.home.list.components
 
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.domain.model.CatalogItem
import com.farbalapps.rinde.domain.model.ShoppingItem as DomainShoppingItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddProductBottomSheet(
    onDismiss: () -> Unit,
    catalogItems: List<CatalogItem>,
    productCategories: List<String>,
    targetGroup: String,
    onProductAdded: (String, String, String, Double, String, String, Boolean) -> Unit, // name, cat, group, qty, unit, emoji, isCustom
    onProductUpdated: (String, String, String, Double, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onShowMessage: (String) -> Unit = {},
    onAddCategory: (String) -> Unit = {}, // This is now for product categories if needed, but usually fixed from catalog
    initialItem: DomainShoppingItem? = null,
    customHistory: List<com.farbalapps.rinde.domain.model.CustomProductHistory> = emptyList()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val defaultCategory = stringResource(id = R.string.category_all)
    val defaultCustomCategory = stringResource(id = R.string.cat_others)
    
    // In the BottomSheet, we select the Product Category (Fruits, etc)
    var selectedProductCategory by remember { mutableStateOf(defaultCategory) }
    
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<CatalogItem?>(null) }
    var quantity by remember { mutableDoubleStateOf(1.0) }
    
    val unitPiece = stringResource(id = R.string.unit_piece)
    val unitKg = stringResource(id = R.string.unit_kg)
    val unitLiter = stringResource(id = R.string.unit_liter)
    val unitPackage = stringResource(id = R.string.unit_package)
    var selectedUnit by remember { mutableStateOf(unitPiece) }
    
    val units = listOf(unitPiece, unitKg, unitLiter, unitPackage)
    val msgAdded = stringResource(R.string.msg_product_added)
    val msgUpdated = stringResource(R.string.msg_product_updated)

    LaunchedEffect(initialItem, catalogItems) {
        initialItem?.let { item ->
            quantity = item.quantity
            selectedUnit = item.unit
            selectedProductCategory = item.category // Pre-select item's category
            if (item.emoji.isNotEmpty()) {
                val matchingItem = catalogItems.find { 
                    it.nombre.equals(item.name, ignoreCase = true) && 
                    it.categoria.equals(item.category, ignoreCase = true) 
                }
                selectedItem = matchingItem ?: CatalogItem(id = 0, nombre = item.name, categoria = item.category, emoji = item.emoji)
                selectedTab = 0
                searchQuery = ""
            } else {
                selectedTab = 1
                customName = item.name
            }
        }
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
                .animateContentSize()
                .padding(bottom = 32.dp)
        ) {
            BottomSheetHeader(
                titleRes = if (initialItem != null) R.string.edit_item_title else R.string.add_items_title,
                onDismiss = onDismiss
            )

            val isEditing = initialItem != null

            if (!isEditing) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text(stringResource(id = R.string.tab_catalog), modifier = Modifier.padding(vertical = 12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text(stringResource(id = R.string.tab_custom), modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            } else {
                // If editing, make sure the title reflects the item name or type
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Product Category Selection (Fruits, Vegetables, etc)
            // Solo mostramos el selector de categorías si no estamos editando o si estamos en el catálogo
            AnimatedVisibility(
                visible = selectedTab == 0 && !isEditing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CategorySelectionRow(
                    categories = productCategories,
                    selectedCategory = selectedProductCategory,
                    onCategorySelected = { selectedProductCategory = it },
                    onAddCategoryClick = {},
                    showAddButton = false,
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
            }

            if (selectedTab == 0) {
                if (isEditing) {
                    // Only show the selected item card when editing
                    selectedItem?.let { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CatalogItemCard(
                                item = item,
                                isSelected = true,
                                onClick = { /* Cannot deselect while editing */ },
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                } else {
                    CatalogTabContent(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        catalogItems = catalogItems,
                        selectedProductCategory = selectedProductCategory,
                        selectedItem = selectedItem,
                        onItemClick = { item ->
                            selectedItem = if (selectedItem?.id == item.id) null else item
                            if (selectedItem != null) searchQuery = ""
                        }
                    )
                }
            } else {
                CustomTabContent(
                    customName = customName,
                    onCustomNameChange = { customName = it },
                    customHistory = if (isEditing) emptyList() else customHistory,
                    onHistoryItemClick = { customName = it }
                )
            }

            // Configuration Section
            val isItemSelected = (selectedTab == 0 && selectedItem != null) || (selectedTab == 1 && customName.isNotBlank()) || initialItem != null
            
            BottomSheetConfigSection(
                visible = isItemSelected,
                quantity = quantity,
                onQuantityChange = { quantity = it },
                selectedUnit = selectedUnit,
                units = units,
                onUnitSelected = { selectedUnit = it },
                onActionClick = {
                    initialItem?.let {
                        // Preserva el grupo original al editar
                        onProductUpdated(it.id, it.name, it.category, quantity, selectedUnit, it.emoji)
                        onShowMessage(msgUpdated.format(it.name))
                        onDismiss()
                    } ?: run {
                        if (selectedTab == 0) {
                            selectedItem?.let {
                                // Usa targetGroup (el grupo activo en la pantalla principal)
                                onProductAdded(it.nombre, it.categoria, targetGroup, quantity, selectedUnit, it.emoji, false)
                                onShowMessage(msgAdded.format(it.nombre))
                                selectedItem = null
                                quantity = 1.0
                                searchQuery = ""
                            }
                        } else {
                            if (customName.isNotBlank()) {
                                // Usa la categoría seleccionada en el Bottom Sheet para items personalizados
                                val categoryToUse = if (selectedProductCategory == defaultCategory) defaultCustomCategory else selectedProductCategory
                                onProductAdded(customName, categoryToUse, targetGroup, quantity, selectedUnit, "", true)
                                onShowMessage(msgAdded.format(customName))
                                customName = ""
                                quantity = 1.0
                            }
                        }
                    }
                },
                isUpdate = initialItem != null
            )
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name ->
                if (name.isNotBlank()) {
                    onAddCategory(name)
                    selectedProductCategory = name
                }
                showAddCategoryDialog = false
            }
        )
    }
}

@Composable
private fun BottomSheetHeader(
    titleRes: Int,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) {
            Text(
                text = stringResource(id = R.string.btn_cancel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        TextButton(onClick = onDismiss) {
            Text(
                text = stringResource(id = R.string.btn_done),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CatalogTabContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    catalogItems: List<CatalogItem>,
    selectedProductCategory: String,
    selectedItem: CatalogItem?,
    onItemClick: (CatalogItem) -> Unit
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedBorderColor = Color.Transparent,
            )
        )

        val filteredItems = catalogItems.filter {
            val matchesSearch = it.categoria.contains(searchQuery, ignoreCase = true) || it.nombre.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedProductCategory == stringResource(R.string.category_all) || it.categoria == selectedProductCategory
            matchesSearch && matchesCategory
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(filteredItems) { item ->
                CatalogItemCard(
                    item = item,
                    isSelected = selectedItem?.id == item.id,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun CustomTabContent(
    customName: String,
    onCustomNameChange: (String) -> Unit,
    customHistory: List<com.farbalapps.rinde.domain.model.CustomProductHistory>,
    onHistoryItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        OutlinedTextField(
            value = customName,
            onValueChange = onCustomNameChange,
            label = { Text(stringResource(id = R.string.add_product_name_hint)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (customHistory.isNotEmpty()) {
            Text(
                text = stringResource(R.string.suggestions_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                customHistory.take(6).forEach { historyItem ->
                    AssistChip(
                        onClick = { onHistoryItemClick(historyItem.name) },
                        label = { Text(historyItem.name) },
                        leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSheetConfigSection(
    visible: Boolean,
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    selectedUnit: String,
    units: List<String>,
    onUnitSelected: (String) -> Unit,
    onActionClick: () -> Unit,
    isUpdate: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuantitySelector(
                        value = quantity,
                        onValueChange = onQuantityChange
                    )

                    UnitSelectorCompact(
                        options = units,
                        selectedOption = selectedUnit,
                        onOptionSelected = onUnitSelected
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = onActionClick,
                        modifier = Modifier.height(48.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        val icon = if (isUpdate) Icons.Default.Save else Icons.Default.Add
                        val labelRes = if (isUpdate) R.string.btn_save_changes else R.string.btn_add_to_list
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = labelRes), 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
