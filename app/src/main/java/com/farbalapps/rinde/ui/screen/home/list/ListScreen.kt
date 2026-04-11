package com.farbalapps.rinde.ui.screen.home.list

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.screen.home.list.components.*


@OptIn(ExperimentalMaterial3Api::class)
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
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                    )
                )

                CategorySelectionRow(
                    categories = uiState.availableGroups,
                    selectedCategory = uiState.selectedFilterGroup,
                    onCategorySelected = { viewModel.setFilterGroup(it) },
                    onCategoryLongClick = { showCategoryActionSheet = it },
                    onAddCategoryClick = { showAddCategoryDialog = true },
                    modifier = Modifier.padding(horizontal = 0.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
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
                        onSwipeStateChange = { isSwiped ->
                            if (isSwiped) viewModel.setSwipedItem(item.id)
                            else if (uiState.swipedItemId == item.id) viewModel.setSwipedItem(null)
                        },
                        onCheckedChange = { isChecked ->
                            viewModel.toggleItemStatus(item, isChecked)
                        },
                        onEdit = { viewModel.startEditing(item) },
                        onDelete = { viewModel.deleteItem(item) }
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
                            onSwipeStateChange = { isSwiped ->
                                if (isSwiped) viewModel.setSwipedItem(item.id)
                                else if (uiState.swipedItemId == item.id) viewModel.setSwipedItem(null)
                            },
                            onCheckedChange = { isChecked ->
                                viewModel.toggleItemStatus(item, isChecked)
                            },
                            onEdit = { viewModel.startEditing(item) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }

        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { name ->
                    viewModel.addCategory(name)
                    showAddCategoryDialog = false
                }
            )
        }

        if (categoryToDelete != null) {
            DeleteGroupDialog(
                groupName = categoryToDelete!!,
                onDismiss = { categoryToDelete = null },
                onConfirm = {
                    viewModel.deleteCategory(categoryToDelete!!)
                    categoryToDelete = null
                }
            )
        }

        if (showCategoryActionSheet != null) {
            CategoryActionBottomSheet(
                categoryName = showCategoryActionSheet!!,
                onDismiss = { showCategoryActionSheet = null },
                onEditClick = {
                    editCategoryName = showCategoryActionSheet
                    showCategoryActionSheet = null
                },
                onReorderClick = {
                    showReorderDialog = true
                    showCategoryActionSheet = null
                },
                onDeleteClick = {
                    categoryToDelete = showCategoryActionSheet
                    showCategoryActionSheet = null
                }
            )
        }

        if (editCategoryName != null) {
            EditCategoryDialog(
                initialName = editCategoryName!!,
                onDismiss = { editCategoryName = null },
                onConfirm = { newName ->
                    viewModel.renameCategory(editCategoryName!!, newName)
                    editCategoryName = null
                }
            )
        }

        if (showReorderDialog) {
            ReorderCategoriesDialog(
                categories = uiState.availableGroups,
                onDismiss = { showReorderDialog = false },
                onConfirm = { newOrder ->
                    viewModel.reorderCategories(newOrder)
                    showReorderDialog = false
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ListScreenPreview() {
    ListScreen()
}