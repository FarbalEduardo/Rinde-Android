package com.farbalapps.rinde.ui.screen.home.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.R
import com.farbalapps.rinde.data.util.JsonAssetReader
import com.farbalapps.rinde.domain.model.CatalogItem
import com.farbalapps.rinde.domain.model.CustomProductHistory
import com.farbalapps.rinde.domain.model.SavedShoppingList
import com.farbalapps.rinde.domain.model.ShoppingItem as DomainShoppingItem
import com.farbalapps.rinde.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    val errorMessage: String? = null,

    // Feature 1: Multi-selection
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<String> = emptySet(),

    // Feature 2: Budget totals
    val activeTotal: Double? = null,
    val completedTotal: Double? = null,
    val budgetCurrency: String = "MXN",
    val budgetProgress: Float = 0f,
    val isBudgetExpanded: Boolean = true,

    // Feature 3: Saved Lists History & Dialogs
    val savedLists: List<SavedShoppingList> = emptyList(),
    val showSavedListsSheet: Boolean = false,
    val selectedSavedListForDetail: SavedShoppingList? = null,
    val showSaveListDialog: Boolean = false,
    val renamingSavedList: SavedShoppingList? = null
)

@HiltViewModel
class ListViewModel @Inject constructor(
    private val getListItemsUseCase: GetListItemsUseCase,
    private val addListItemUseCase: AddListItemUseCase,
    private val deleteListItemUseCase: DeleteListItemUseCase,
    private val deleteMultipleItemsUseCase: DeleteMultipleItemsUseCase,
    private val toggleListItemUseCase: ToggleListItemUseCase,
    private val updateListItemUseCase: UpdateListItemUseCase,
    private val getCustomProductHistoryUseCase: GetCustomProductHistoryUseCase,
    private val saveCustomProductHistoryUseCase: com.farbalapps.rinde.domain.usecase.SaveCustomProductHistoryUseCase,
    private val deleteCustomProductHistoryUseCase: com.farbalapps.rinde.domain.usecase.DeleteCustomProductHistoryUseCase,
    private val getCategoriesUseCase: com.farbalapps.rinde.domain.usecase.GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val renameCategoryUseCase: RenameCategoryUseCase,
    private val reorderCategoriesUseCase: ReorderCategoriesUseCase,
    private val syncItemsUseCase: SyncItemsUseCase,
    private val syncCategoriesUseCase: SyncCategoriesUseCase,
    private val saveCurrentListUseCase: SaveCurrentListUseCase,
    private val getSavedListsUseCase: GetSavedListsUseCase,
    private val deleteSavedListUseCase: DeleteSavedListUseCase,
    private val loadSavedListUseCase: LoadSavedListUseCase,
    private val renameSavedListUseCase: RenameSavedListUseCase,
    private val syncSavedListsUseCase: SyncSavedListsUseCase,
    private val jsonAssetReader: JsonAssetReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilterGroup = MutableStateFlow("All")

    private val pendingHighlightIds = mutableSetOf<String>()

    init {
        syncData()
        observeItems()
        observeCustomHistory()
        observeGroups()
        observeSavedLists()
    }

    private fun syncData() {
        viewModelScope.launch {
            syncCategoriesUseCase()
            syncItemsUseCase()
            syncSavedListsUseCase()
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

    private fun observeSavedLists() {
        viewModelScope.launch {
            getSavedListsUseCase().collect { lists ->
                _uiState.update { it.copy(savedLists = lists) }
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
            combine(
                getListItemsUseCase(),
                _searchQuery,
                _selectedFilterGroup
            ) { items, query, group ->
                Triple(items, query, group)
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { (items, query, group) ->
                    _uiState.update { currentState ->
                        val uniqueItems = items.distinctBy { it.id }
                        val filteredItems = filterItems(uniqueItems, group, query)
                        val active = filteredItems.filter { !it.isCompleted }
                        val completed = filteredItems.filter { it.isCompleted }
                        val totals = calculateTotals(active, completed, uniqueItems)

                        currentState.copy(
                            activeItems = active,
                            completedItems = completed,
                            activeTotal = totals.activeTotal,
                            completedTotal = totals.completedTotal,
                            budgetCurrency = totals.currency,
                            budgetProgress = totals.progress,
                            searchQuery = query,
                            selectedFilterGroup = group,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    private fun filterItems(items: List<DomainShoppingItem>, group: String, query: String): List<DomainShoppingItem> {
        val groupFiltered = if (group == "All") items else items.filter { it.listGroup == group }
        return if (query.isBlank()) {
            groupFiltered
        } else {
            groupFiltered.filter { 
                it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
            }
        }
    }

    private data class BudgetTotals(
        val activeTotal: Double?,
        val completedTotal: Double?,
        val currency: String,
        val progress: Float
    )

    private fun calculateTotals(
        active: List<DomainShoppingItem>, 
        completed: List<DomainShoppingItem>, 
        allItems: List<DomainShoppingItem>
    ): BudgetTotals {
        val activePriced = active.filter { it.price != null }
        val completedPriced = completed.filter { it.price != null }

        val actTotal = if (activePriced.isNotEmpty()) activePriced.sumOf { (it.price ?: 0.0) * it.quantity } else null
        val compTotal = if (completedPriced.isNotEmpty()) completedPriced.sumOf { (it.price ?: 0.0) * it.quantity } else null

        val totalItemsCount = active.size + completed.size
        val progress = if (totalItemsCount > 0) completed.size.toFloat() / totalItemsCount.toFloat() else 0f
        val currency = allItems.firstOrNull { it.price != null }?.currency ?: "MXN"

        return BudgetTotals(actTotal, compTotal, currency, progress)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilterGroup(group: String) {
        _selectedFilterGroup.value = group
    }

    fun addItem(
        name: String,
        category: String,
        listGroup: String,
        quantity: Double = 1.0,
        unit: String = "Pieza",
        emoji: String = "",
        isCustom: Boolean = false,
        price: Double? = null,
        currency: String = "MXN"
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
                    listGroup = listGroup,
                    price = price,
                    currency = currency
                )
                addListItemUseCase(newItem)
                pendingHighlightIds.add(newId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Sync Error: ${e.message}") }
            }
        }
    }

    fun deleteCustomHistory(name: String) {
        viewModelScope.launch {
            try {
                deleteCustomProductHistoryUseCase(name)
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun updateItemQuantity(item: DomainShoppingItem, delta: Double) {
        val newQuantity = (item.quantity + delta).coerceAtLeast(0.5)
        if (newQuantity == item.quantity) return
        viewModelScope.launch {
            try {
                updateListItemUseCase(item.copy(quantity = newQuantity))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Update Quantity Error: ${e.message}") }
            }
        }
    }

    fun setItemQuantity(item: DomainShoppingItem, quantity: Double) {
        val newQuantity = quantity.coerceAtLeast(0.5)
        if (newQuantity == item.quantity) return
        viewModelScope.launch {
            try {
                updateListItemUseCase(item.copy(quantity = newQuantity))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Update Quantity Error: ${e.message}") }
            }
        }
    }

    fun updateItem(
        id: String,
        name: String,
        category: String,
        quantity: Double,
        unit: String,
        emoji: String,
        price: Double? = null,
        currency: String = "MXN"
    ) {
        viewModelScope.launch {
            try {
                val editing = _uiState.value.editingItem
                val itemToUpdate = DomainShoppingItem(
                    id = id,
                    name = name,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    emoji = emoji,
                    listGroup = editing?.listGroup ?: "All",
                    userId = editing?.userId ?: "",
                    isCompleted = editing?.isCompleted ?: false,
                    price = price,
                    currency = currency
                )
                if (emoji.isEmpty() && name.isNotBlank()) {
                    saveCustomProductHistoryUseCase(name, category)
                }
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

    // ── Feature 1: Multi-selection ──────────────────────────────────────────

    fun toggleSelectionMode(enabled: Boolean, initialItemId: String? = null) {
        _uiState.update { state ->
            val initialSelected = if (enabled && initialItemId != null) setOf(initialItemId) else emptySet()
            state.copy(
                isSelectionMode = enabled,
                selectedItemIds = initialSelected
            )
        }
    }

    fun toggleItemSelection(itemId: String) {
        _uiState.update { state ->
            val updated = if (state.selectedItemIds.contains(itemId)) {
                state.selectedItemIds - itemId
            } else {
                state.selectedItemIds + itemId
            }
            state.copy(
                selectedItemIds = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAllToggle() {
        _uiState.update { state ->
            val allIds = (state.activeItems + state.completedItems).map { it.id }.toSet()
            val isAllSelected = state.selectedItemIds.size == allIds.size
            state.copy(
                selectedItemIds = if (isAllSelected) emptySet() else allIds,
                isSelectionMode = !isAllSelected
            )
        }
    }

    fun deleteSelectedItems() {
        val selectedIds = _uiState.value.selectedItemIds
        if (selectedIds.isEmpty()) return

        val allItems = _uiState.value.activeItems + _uiState.value.completedItems
        val itemsToDelete = allItems.filter { selectedIds.contains(it.id) }

        viewModelScope.launch {
            try {
                deleteMultipleItemsUseCase(itemsToDelete)
                _uiState.update { it.copy(isSelectionMode = false, selectedItemIds = emptySet()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Delete Batch Error: ${e.message}") }
            }
        }
    }

    // ── Feature 2: Budget ───────────────────────────────────────────────────

    fun toggleBudgetExpanded() {
        _uiState.update { it.copy(isBudgetExpanded = !it.isBudgetExpanded) }
    }

    // ── Feature 3: Saved Lists ──────────────────────────────────────────────

    fun openSaveListDialog() {
        _uiState.update { it.copy(showSaveListDialog = true) }
    }

    fun closeSaveListDialog() {
        _uiState.update { it.copy(showSaveListDialog = false) }
    }

    fun saveCurrentList(name: String, clearAfterSave: Boolean = false) {
        val completedItems = _uiState.value.completedItems
        if (completedItems.isEmpty()) return

        viewModelScope.launch {
            try {
                saveCurrentListUseCase(name, completedItems)
                if (clearAfterSave) {
                    deleteMultipleItemsUseCase(completedItems)
                }
                closeSaveListDialog()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Save List Error: ${e.message}") }
            }
        }
    }

    fun updateItemPrice(itemId: String, price: Double?, currency: String) {
        val item = (_uiState.value.activeItems + _uiState.value.completedItems).find { it.id == itemId } ?: return
        viewModelScope.launch {
            try {
                updateListItemUseCase(item.copy(price = price, currency = currency))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Price Update Error: ${e.message}") }
            }
        }
    }

    fun openSavedListsSheet() {
        _uiState.update { it.copy(showSavedListsSheet = true) }
    }

    fun closeSavedListsSheet() {
        _uiState.update { it.copy(showSavedListsSheet = false) }
    }

    fun selectSavedListForDetail(savedList: SavedShoppingList?) {
        _uiState.update { it.copy(selectedSavedListForDetail = savedList) }
    }

    fun deleteSavedList(savedList: SavedShoppingList) {
        viewModelScope.launch {
            try {
                deleteSavedListUseCase(savedList.id)
                if (_uiState.value.selectedSavedListForDetail?.id == savedList.id) {
                    selectSavedListForDetail(null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Delete Saved List Error: ${e.message}") }
            }
        }
    }

    fun startRenamingSavedList(savedList: SavedShoppingList?) {
        _uiState.update { it.copy(renamingSavedList = savedList) }
    }

    fun cancelRenamingSavedList() {
        _uiState.update { it.copy(renamingSavedList = null) }
    }

    fun renameSavedList(newName: String) {
        val targetList = _uiState.value.renamingSavedList ?: return
        viewModelScope.launch {
            try {
                renameSavedListUseCase(targetList.id, newName)
                _uiState.update { it.copy(renamingSavedList = null) }
                // Update detail if open
                _uiState.value.selectedSavedListForDetail?.let { currentDetail ->
                    if (currentDetail.id == targetList.id) {
                        _uiState.update { it.copy(selectedSavedListForDetail = currentDetail.copy(name = newName)) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Rename List Error: ${e.message}") }
            }
        }
    }

    fun loadSavedList(savedList: SavedShoppingList, mode: LoadSavedListMode, group: String) {
        viewModelScope.launch {
            try {
                // If mode is REPLACE_NEW, create category if it doesn't exist
                if (mode == LoadSavedListMode.REPLACE_NEW && group.isNotBlank()) {
                    if (!_uiState.value.availableGroups.contains(group)) {
                        addCategoryUseCase(group)
                    }
                    setFilterGroup(group)
                }

                loadSavedListUseCase(savedList, mode, group)
                selectSavedListForDetail(null)
                closeSavedListsSheet()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Load Saved List Error: ${e.message}") }
            }
        }
    }

    // ── Swipes & Edit state ─────────────────────────────────────────────────

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
            val currentCategories = getCategoriesUseCase().first()
            val orderedCategories = reorderedNames.mapNotNull { name ->
                currentCategories.find { it.name == name }
            }
            reorderCategoriesUseCase(orderedCategories)
        }
    }
}
