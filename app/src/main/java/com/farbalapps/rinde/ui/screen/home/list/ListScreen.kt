package com.farbalapps.rinde.ui.screen.home.list

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.home.list.components.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: ListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var showCategoryActionSheet by remember { mutableStateOf<String?>(null) }
    var editCategoryName by remember { mutableStateOf<String?>(null) }
    var showReorderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCatalog(context)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 840.dp)
                .padding(horizontal = 16.dp)
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header con búsqueda, categorías y acciones (se oculta al hacer scroll hacia arriba)
            item {
                val totalItemsCount = uiState.activeItems.size + uiState.completedItems.size
                ListStickyHeader(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    categories = uiState.availableGroups,
                    selectedCategory = uiState.selectedFilterGroup,
                    onCategorySelected = { viewModel.setFilterGroup(it) },
                    onCategoryLongClick = { showCategoryActionSheet = it },
                    onAddCategoryClick = { showAddCategoryDialog = true },
                    isSelectionMode = uiState.isSelectionMode,
                    selectedCount = uiState.selectedItemIds.size,
                    totalCount = totalItemsCount,
                    onSelectAllToggle = { viewModel.selectAllToggle() },
                    onCancelSelection = { viewModel.toggleSelectionMode(false) },
                    onSaveList = { 
                        if (totalItemsCount == 0) {
                            android.widget.Toast.makeText(context, "La lista está vacía y no se puede guardar.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.openSaveListDialog() 
                        }
                    },
                    onOpenHistory = { viewModel.openSavedListsSheet() }
                )
            }

            // Presupuesto pegado arriba (Sticky Header) si hay precios
            stickyHeader {
                val hasAnyPrice = uiState.activeTotal != null || uiState.completedTotal != null
                if (hasAnyPrice) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        BudgetSummaryCard(
                            activeCount = uiState.activeItems.size,
                            activeTotal = uiState.activeTotal,
                            completedCount = uiState.completedItems.size,
                            completedTotal = uiState.completedTotal,
                            currency = uiState.budgetCurrency,
                            progress = uiState.budgetProgress,
                            isExpanded = uiState.isBudgetExpanded,
                            onToggleExpand = { viewModel.toggleBudgetExpanded() }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item { 
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator() 
                    }
                }
            } else if (uiState.activeItems.isEmpty() && uiState.completedItems.isEmpty()) {
                item { EmptyStateView() }
            } else {
                items(uiState.activeItems, key = { it.id }) { item ->
                    ShoppingListItem(
                        item = item,
                        isHighlighted = uiState.newlyAddedItemIds.contains(item.id),
                        isSwiped = uiState.swipedItemId == item.id,
                        isSelectionMode = uiState.isSelectionMode,
                        isSelected = uiState.selectedItemIds.contains(item.id),
                        onSwipeStateChange = { isSwiped ->
                            if (isSwiped) viewModel.setSwipedItem(item.id)
                            else if (uiState.swipedItemId == item.id) viewModel.setSwipedItem(null)
                        },
                        onCheckedChange = { isChecked ->
                            viewModel.toggleItemStatus(item, isChecked)
                        },
                        onLongClick = {
                            viewModel.toggleSelectionMode(true, item.id)
                        },
                        onSelectionToggle = {
                            viewModel.toggleItemSelection(item.id)
                        },
                        onEdit = { viewModel.startEditing(item) },
                        onDelete = { viewModel.deleteItem(item) },
                        onIncrement = { viewModel.updateItemQuantity(item, 0.5) },
                        onDecrement = { viewModel.updateItemQuantity(item, -0.5) },
                        onQuantitySet = { newQty -> viewModel.setItemQuantity(item, newQty) },
                        onSetPrice = { price, currency ->
                            viewModel.updateItemPrice(item.id, price, currency)
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                if (uiState.completedItems.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.filter_completed).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(uiState.completedItems, key = { it.id }) { item ->
                        ShoppingListItem(
                            item = item,
                            isHighlighted = uiState.newlyAddedItemIds.contains(item.id),
                            isSwiped = uiState.swipedItemId == item.id,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = uiState.selectedItemIds.contains(item.id),
                            onSwipeStateChange = { isSwiped ->
                                if (isSwiped) viewModel.setSwipedItem(item.id)
                                else if (uiState.swipedItemId == item.id) viewModel.setSwipedItem(null)
                            },
                            onCheckedChange = { isChecked ->
                                viewModel.toggleItemStatus(item, isChecked)
                            },
                            onLongClick = {
                                viewModel.toggleSelectionMode(true, item.id)
                            },
                            onSelectionToggle = {
                                viewModel.toggleItemSelection(item.id)
                            },
                            onEdit = { viewModel.startEditing(item) },
                            onDelete = { viewModel.deleteItem(item) },
                            onIncrement = { viewModel.updateItemQuantity(item, 0.5) },
                            onDecrement = { viewModel.updateItemQuantity(item, -0.5) },
                            onQuantitySet = { newQty -> viewModel.setItemQuantity(item, newQty) },
                            onSetPrice = { price, currency ->
                                viewModel.updateItemPrice(item.id, price, currency)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Selection mode bottom action bar
        SelectionBottomBar(
            isVisible = uiState.isSelectionMode,
            selectedCount = uiState.selectedItemIds.size,
            onDeleteClick = { viewModel.deleteSelectedItems() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp)
        )

        // Dialogs & BottomSheets
        ListScreenDialogs(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            showAddCategoryDialog = showAddCategoryDialog,
            onShowAddCategoryDialogChange = { showAddCategoryDialog = it },
            categoryToDelete = categoryToDelete,
            onCategoryToDeleteChange = { categoryToDelete = it },
            showCategoryActionSheet = showCategoryActionSheet,
            onShowCategoryActionSheetChange = { showCategoryActionSheet = it },
            editCategoryName = editCategoryName,
            onEditCategoryNameChange = { editCategoryName = it },
            showReorderDialog = showReorderDialog,
            onShowReorderDialogChange = { showReorderDialog = it }
        )
    }
}

@Composable
private fun ListScreenDialogs(
    uiState: ListUiState,
    viewModel: ListViewModel,
    context: android.content.Context,
    showAddCategoryDialog: Boolean,
    onShowAddCategoryDialogChange: (Boolean) -> Unit,
    categoryToDelete: String?,
    onCategoryToDeleteChange: (String?) -> Unit,
    showCategoryActionSheet: String?,
    onShowCategoryActionSheetChange: (String?) -> Unit,
    editCategoryName: String?,
    onEditCategoryNameChange: (String?) -> Unit,
    showReorderDialog: Boolean,
    onShowReorderDialogChange: (Boolean) -> Unit
) {
    if (uiState.showSaveListDialog) {
        val allItemsCount = uiState.activeItems.size + uiState.completedItems.size
        val totalPriceSum = ((uiState.activeTotal ?: 0.0) + (uiState.completedTotal ?: 0.0)).let { if (it > 0) it else null }

        SaveListDialog(
            totalItems = allItemsCount,
            totalPrice = totalPriceSum,
            currency = uiState.budgetCurrency,
            onDismiss = { viewModel.closeSaveListDialog() },
            onConfirm = { name, clearAfterSave ->
                viewModel.saveCurrentList(name, clearAfterSave)
            }
        )
    }

    if (uiState.showSavedListsSheet) {
        SavedListsBottomSheet(
            savedLists = uiState.savedLists,
            onDismiss = { viewModel.closeSavedListsSheet() },
            onSelectList = { list ->
                viewModel.selectSavedListForDetail(list)
            },
            onDeleteList = { list ->
                viewModel.deleteSavedList(list)
            }
        )
    }

    uiState.selectedSavedListForDetail?.let { detailList ->
        SavedListDetailSheet(
            savedList = detailList,
            availableGroups = uiState.availableGroups,
            onDismiss = { viewModel.selectSavedListForDetail(null) },
            onRenameClick = { viewModel.startRenamingSavedList(detailList) },
            onLoadList = { list, mode, group ->
                viewModel.loadSavedList(list, mode, group)
            }
        )
    }

    uiState.renamingSavedList?.let { targetList ->
        RenameListDialog(
            initialName = targetList.name,
            onDismiss = { viewModel.startRenamingSavedList(targetList) }, // cancels if null passed? No, pass null in state
            onConfirm = { newName ->
                viewModel.renameSavedList(newName)
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { onShowAddCategoryDialogChange(false) },
            onConfirm = { name ->
                viewModel.addCategory(name)
                onShowAddCategoryDialogChange(false)
            }
        )
    }

    if (categoryToDelete != null) {
        DeleteGroupDialog(
            groupName = categoryToDelete,
            onDismiss = { onCategoryToDeleteChange(null) },
            onConfirm = {
                viewModel.deleteCategory(categoryToDelete)
                onCategoryToDeleteChange(null)
            }
        )
    }

    if (showCategoryActionSheet != null) {
        CategoryActionBottomSheet(
            categoryName = showCategoryActionSheet,
            onDismiss = { onShowCategoryActionSheetChange(null) },
            onEditClick = {
                onEditCategoryNameChange(showCategoryActionSheet)
                onShowCategoryActionSheetChange(null)
            },
            onReorderClick = {
                onShowReorderDialogChange(true)
                onShowCategoryActionSheetChange(null)
            },
            onDeleteClick = {
                onCategoryToDeleteChange(showCategoryActionSheet)
                onShowCategoryActionSheetChange(null)
            }
        )
    }

    if (editCategoryName != null) {
        EditCategoryDialog(
            initialName = editCategoryName,
            onDismiss = { onEditCategoryNameChange(null) },
            onConfirm = { newName ->
                viewModel.renameCategory(editCategoryName, newName)
                onEditCategoryNameChange(null)
            }
        )
    }

    if (showReorderDialog) {
        ReorderCategoriesDialog(
            categories = uiState.availableGroups,
            onDismiss = { onShowReorderDialogChange(false) },
            onConfirm = { newOrder ->
                viewModel.reorderCategories(newOrder)
                onShowReorderDialogChange(false)
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ListScreenPreview() {
    ListScreen()
}