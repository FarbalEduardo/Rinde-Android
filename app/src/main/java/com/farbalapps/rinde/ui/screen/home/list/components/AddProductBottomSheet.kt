package com.farbalapps.rinde.ui.screen.home.list.components
 
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
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
    onProductAdded: (name: String, category: String, listGroup: String, quantity: Double, unit: String, emoji: String, isCustom: Boolean, price: Double?, currency: String) -> Unit,
    onProductUpdated: (id: String, name: String, category: String, quantity: Double, unit: String, emoji: String, price: Double?, currency: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onShowMessage: (String) -> Unit = {},
    onAddCategory: (String) -> Unit = {}, // This is now for product categories if needed, but usually fixed from catalog
    initialItem: DomainShoppingItem? = null,
    customHistory: List<com.farbalapps.rinde.domain.model.CustomProductHistory> = emptyList(),
    onDeleteCustomHistory: (String) -> Unit = {}
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

    var priceText by remember { mutableStateOf("") }
    val currency = "MXN"

    LaunchedEffect(initialItem, catalogItems) {
        initialItem?.let { item ->
            quantity = item.quantity
            selectedUnit = item.unit
            selectedProductCategory = item.category // Pre-select item's category
            priceText = item.price?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
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
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            // Scrollable Content Area (Header, Tabs, Catalog/Custom, Price)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                BottomSheetHeader(
                    titleRes = if (initialItem != null) R.string.edit_item_title else R.string.add_items_title,
                    onDismiss = onDismiss
                )

                val isEditing = initialItem != null

                if (!isEditing) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text(stringResource(id = R.string.tab_catalog), modifier = Modifier.padding(vertical = 10.dp))
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text(stringResource(id = R.string.tab_custom), modifier = Modifier.padding(vertical = 10.dp))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Product Category Selection (Fruits, Vegetables, etc)
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
                        selectedItem?.let { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CatalogItemCard(
                                    item = item,
                                    isSelected = true,
                                    onClick = { },
                                    modifier = Modifier.size(90.dp)
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
                            }
                        )
                    }
                } else {
                    CustomTabContent(
                        customName = customName,
                        onCustomNameChange = { customName = it },
                        customHistory = if (isEditing) emptyList() else customHistory,
                        onHistoryItemClick = { customName = it },
                        onDeleteHistoryItem = onDeleteCustomHistory
                    )
                }
            }

            // Pinned Action & Config Section at bottom (Price, Quantity, Unit, CTA Button) — ALWAYS 100% VISIBLE!
            val isItemSelected = (selectedTab == 0 && selectedItem != null) ||
                                 (selectedTab == 1 && customName.isNotBlank()) ||
                                 initialItem != null

            val parsedPrice = priceText.toDoubleOrNull()

            BottomSheetConfigSection(
                enabled = isItemSelected,
                priceText = priceText,
                onPriceTextChange = { priceText = it.filter { char -> char.isDigit() || char == '.' } },
                quantity = quantity,
                onQuantityChange = { quantity = it },
                selectedUnit = selectedUnit,
                units = units,
                onUnitSelected = { selectedUnit = it },
                onActionClick = {
                    initialItem?.let {
                        onProductUpdated(it.id, it.name, it.category, quantity, selectedUnit, it.emoji, parsedPrice, currency)
                        onShowMessage(msgUpdated.format(it.name))
                        onDismiss()
                    } ?: run {
                        if (selectedTab == 0) {
                            selectedItem?.let {
                                onProductAdded(it.nombre, it.categoria, targetGroup, quantity, selectedUnit, it.emoji, false, parsedPrice, currency)
                                onShowMessage(msgAdded.format(it.nombre))
                                selectedItem = null
                                quantity = 1.0
                                priceText = ""
                                searchQuery = ""
                            }
                        } else {
                            if (customName.isNotBlank()) {
                                val categoryToUse = if (selectedProductCategory == defaultCategory) defaultCustomCategory else selectedProductCategory
                                onProductAdded(customName, categoryToUse, targetGroup, quantity, selectedUnit, "", true, parsedPrice, currency)
                                onShowMessage(msgAdded.format(customName))
                                customName = ""
                                quantity = 1.0
                                priceText = ""
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
            modifier = Modifier.heightIn(max = 210.dp)
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomTabContent(
    customName: String,
    onCustomNameChange: (String) -> Unit,
    customHistory: List<com.farbalapps.rinde.domain.model.CustomProductHistory>,
    onHistoryItemClick: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit
) {
    var itemToDelete by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(24.dp)) {
        OutlinedTextField(
            value = customName,
            onValueChange = onCustomNameChange,
            label = { Text(stringResource(id = R.string.add_product_name_hint)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        CustomHistorySection(
            customHistory = customHistory,
            onHistoryItemClick = onHistoryItemClick,
            onItemToDelete = { itemToDelete = it }
        )
    }

    itemToDelete?.let { item ->
        DeleteHistoryDialog(
            itemToDelete = item,
            onDelete = {
                onDeleteHistoryItem(item)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomHistorySection(
    customHistory: List<com.farbalapps.rinde.domain.model.CustomProductHistory>,
    onHistoryItemClick: (String) -> Unit,
    onItemToDelete: (String) -> Unit
) {
    if (customHistory.isNotEmpty()) {
        Text(
            text = stringResource(R.string.suggestions_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            customHistory.take(6).forEach { historyItem ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onHistoryItemClick(historyItem.name) },
                                onLongClick = { onItemToDelete(historyItem.name) }
                            )
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(historyItem.name, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteHistoryDialog(
    itemToDelete: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar sugerencia") },
        text = { Text("¿Deseas eliminar \"$itemToDelete\" de tu historial de productos?") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun BottomSheetConfigSection(
    enabled: Boolean,
    priceText: String,
    onPriceTextChange: (String) -> Unit,
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    selectedUnit: String,
    units: List<String>,
    onUnitSelected: (String) -> Unit,
    onActionClick: () -> Unit,
    isUpdate: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        ConfigInputRow(
            quantity = quantity,
            onQuantityChange = onQuantityChange,
            selectedUnit = selectedUnit,
            units = units,
            onUnitSelected = onUnitSelected,
            priceText = priceText,
            onPriceTextChange = onPriceTextChange
        )

        ConfigActionBtn(
            enabled = enabled,
            isUpdate = isUpdate,
            onActionClick = onActionClick
        )
    }
}

@Composable
private fun ConfigInputRow(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    selectedUnit: String,
    units: List<String>,
    onUnitSelected: (String) -> Unit,
    priceText: String,
    onPriceTextChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1.1f)) {
            Text(
                text = "Cantidad",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            QuantitySelector(
                value = quantity,
                onValueChange = onQuantityChange,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }

        Column(modifier = Modifier.weight(0.9f)) {
            Text(
                text = "Unidad",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            UnitSelectorCompact(
                options = units,
                selectedOption = selectedUnit,
                onOptionSelected = onUnitSelected,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Precio",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = priceText,
                onValueChange = onPriceTextChange,
                placeholder = { Text("0.00", style = MaterialTheme.typography.bodyMedium) },
                prefix = { Text("$", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun ConfigActionBtn(
    enabled: Boolean,
    isUpdate: Boolean,
    onActionClick: () -> Unit
) {
    Button(
        onClick = onActionClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        val icon = if (isUpdate) Icons.Default.Save else Icons.Default.Add
        val labelRes = if (isUpdate) R.string.btn_save_changes else R.string.btn_add_to_list
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
