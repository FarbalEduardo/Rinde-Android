package com.farbalapps.rinde.ui.screen.home.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.data.util.JsonAssetReader
import com.farbalapps.rinde.domain.model.CatalogItem
import com.farbalapps.rinde.domain.model.ShoppingItem as DomainShoppingItem
import com.farbalapps.rinde.domain.usecase.AddListItemUseCase
import com.farbalapps.rinde.domain.usecase.DeleteListItemUseCase
import com.farbalapps.rinde.domain.usecase.GetListItemsUseCase
import com.farbalapps.rinde.domain.usecase.ToggleListItemUseCase
import com.farbalapps.rinde.domain.usecase.UpdateListItemUseCase
import com.farbalapps.rinde.domain.usecase.GetCustomProductHistoryUseCase
import com.farbalapps.rinde.domain.usecase.SaveCustomProductHistoryUseCase
import com.farbalapps.rinde.domain.usecase.GetCategoriesUseCase
import com.farbalapps.rinde.domain.usecase.AddCategoryUseCase
import com.farbalapps.rinde.domain.usecase.DeleteCategoryUseCase
import com.farbalapps.rinde.domain.usecase.SyncItemsUseCase
import com.farbalapps.rinde.domain.usecase.SyncCategoriesUseCase
import com.farbalapps.rinde.domain.model.CustomProductHistory
import com.farbalapps.rinde.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import com.farbalapps.rinde.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListUiState(
    val activeItems: List<DomainShoppingItem> = emptyList(),
    val completedItems: List<DomainShoppingItem> = emptyList(),
    val catalogItems: List<CatalogItem> = emptyList(),
    val catalogCategories: List<String> = emptyList(),
    val availableGroups: List<String> = listOf("All"),
    val selectedFilterGroup: String = "All",
    val customProductsHistory: List<CustomProductHistory> = emptyList(),
    val swipedItemId: String? = null,
    val editingItem: DomainShoppingItem? = null,
    val newlyAddedItemIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ListViewModel @Inject constructor(
    private val getListItemsUseCase: GetListItemsUseCase,
    private val addListItemUseCase: AddListItemUseCase,
    private val deleteListItemUseCase: DeleteListItemUseCase,
    private val toggleListItemUseCase: ToggleListItemUseCase,
    private val updateListItemUseCase: UpdateListItemUseCase,
    private val getCustomProductHistoryUseCase: GetCustomProductHistoryUseCase,
    private val saveCustomProductHistoryUseCase: SaveCustomProductHistoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val renameCategoryUseCase: com.farbalapps.rinde.domain.usecase.RenameCategoryUseCase,
    private val reorderCategoriesUseCase: com.farbalapps.rinde.domain.usecase.ReorderCategoriesUseCase,
    private val syncItemsUseCase: SyncItemsUseCase,
    private val syncCategoriesUseCase: SyncCategoriesUseCase,
    private val jsonAssetReader: JsonAssetReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val pendingHighlightIds = mutableSetOf<String>()

    init {
        syncData()
        observeItems()
        observeCustomHistory()
        observeGroups()
    }

    private fun syncData() {
        viewModelScope.launch {
            syncCategoriesUseCase()
            syncItemsUseCase()
        }
    }

    private fun observeGroups() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { groups ->
                _uiState.update { currentState ->
                    val allGroups = listOf("All") + groups.map { it.name }
                    currentState.copy(availableGroups = allGroups)
                }
            }
        }
    }

    private fun observeCustomHistory() {
        viewModelScope.launch {
            getCustomProductHistoryUseCase().collect { history ->
                _uiState.update { it.copy(customProductsHistory = history) }
            }
        }
    }

    fun loadCatalog(context: Context) {
        viewModelScope.launch {
            val language = java.util.Locale.getDefault().language
            val fileName = if (language == "en") "catalog-en.json" else "catalog.json"
            val items = jsonAssetReader.readCatalogFromAssets(context, fileName)
            val catalogs = items.distinctBy { it.categoria }.map { it.categoria }
            
            _uiState.update { currentState ->
                currentState.copy(
                    catalogItems = items,
                    catalogCategories = listOf(context.getString(R.string.category_all)) + catalogs
                ) 
            }
        }
    }


    private fun observeItems() {
        viewModelScope.launch {
            getListItemsUseCase()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { items ->
                    _uiState.update { currentState ->
                        val groupFiltered = if (currentState.selectedFilterGroup == "All") {
                            items
                        } else {
                            items.filter { it.listGroup == currentState.selectedFilterGroup }
                        }
                        
                        val searchFiltered = if (currentState.searchQuery.isBlank()) {
                            groupFiltered
                        } else {
                            groupFiltered.filter { 
                                it.name.contains(currentState.searchQuery, ignoreCase = true) ||
                                it.category.contains(currentState.searchQuery, ignoreCase = true)
                            }
                        }
                        
                        currentState.copy(
                            activeItems = searchFiltered.filter { !it.isCompleted },
                            completedItems = searchFiltered.filter { it.isCompleted },
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        observeItems()
    }

    fun setFilterGroup(group: String) {
        _uiState.update { it.copy(selectedFilterGroup = group) }
        observeItems() // Re-trigger filter
    }

    fun addItem(
        name: String,
        category: String,
        listGroup: String,
        quantity: Double = 1.0,
        unit: String = "Pieza",
        emoji: String = "",
        isCustom: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                if (isCustom) {
                    saveCustomProductHistoryUseCase(name, category)
                }
                val newId = java.util.UUID.randomUUID().toString()
                val newItem = DomainShoppingItem(
                    id = newId,
                    name = name,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    emoji = emoji,
                    listGroup = listGroup
                )
                addListItemUseCase(newItem)
                
                // Add to pending highlights to be triggered when Bottom Sheet closes
                pendingHighlightIds.add(newId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Sync Error: ${e.message}") }
            }
        }
    }

    fun updateItem(
        id: String,
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        emoji: String
    ) {
        viewModelScope.launch {
            try {
                val itemToUpdate = DomainShoppingItem(
                    id = id,
                    name = name,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    emoji = emoji,
                    listGroup = _uiState.value.editingItem?.listGroup ?: "All",
                    userId = _uiState.value.editingItem?.userId ?: ""
                )
                updateListItemUseCase(itemToUpdate)
                stopEditing()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Update Error: ${e.message}") }
            }
        }
    }

    fun toggleItemStatus(item: DomainShoppingItem, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                toggleListItemUseCase(item, isCompleted)
                if (_uiState.value.swipedItemId == item.id) setSwipedItem(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Sync Error: ${e.message}") }
            }
        }
    }

    fun deleteItem(item: DomainShoppingItem) {
        viewModelScope.launch {
            try {
                deleteListItemUseCase(item)
                if (_uiState.value.swipedItemId == item.id) setSwipedItem(null)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Delete Sync Error: ${e.message}") }
            }
        }
    }

    fun setSwipedItem(itemId: String?) {
        _uiState.update { it.copy(swipedItemId = itemId) }
    }

    fun startEditing(item: DomainShoppingItem) {
        _uiState.update { it.copy(editingItem = item) }
    }

    fun stopEditing() {
        _uiState.update { it.copy(editingItem = null) }
    }

    fun triggerPendingHighlights() {
        if (pendingHighlightIds.isEmpty()) return
        
        val idsToHighlight = pendingHighlightIds.toSet()
        pendingHighlightIds.clear()

        _uiState.update { it.copy(newlyAddedItemIds = it.newlyAddedItemIds + idsToHighlight) }
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            _uiState.update { it.copy(newlyAddedItemIds = it.newlyAddedItemIds - idsToHighlight) }
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            addCategoryUseCase(name)
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            deleteCategoryUseCase(name)
            if (_uiState.value.selectedFilterGroup == name) {
                setFilterGroup("All")
            }
        }
    }

    fun renameCategory(oldName: String, newName: String) {
        viewModelScope.launch {
            renameCategoryUseCase(oldName, newName)
            if (_uiState.value.selectedFilterGroup == oldName) {
                setFilterGroup(newName)
            }
        }
    }

    fun reorderCategories(reorderedNames: List<String>) {
        viewModelScope.launch {
            // Need to fetch current to get full Category objects
            val currentCategories = getCategoriesUseCase().first()
            val orderedCategories = reorderedNames.mapNotNull { name ->
                currentCategories.find { it.name == name }
            }
            reorderCategoriesUseCase(orderedCategories)
        }
    }
}
