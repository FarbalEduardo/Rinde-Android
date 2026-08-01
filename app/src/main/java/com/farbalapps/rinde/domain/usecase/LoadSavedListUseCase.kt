package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.SavedShoppingList
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import java.util.UUID
import javax.inject.Inject

enum class LoadSavedListMode {
    MERGE_CURRENT,
    ASSIGN_GROUP,
    REPLACE_NEW
}

/**
 * Use case to import items from a saved shopping list into the active list.
 */
class LoadSavedListUseCase @Inject constructor(
    private val listRepository: ListRepository
) {
    suspend operator fun invoke(
        savedList: SavedShoppingList,
        mode: LoadSavedListMode,
        targetGroup: String = "All"
    ) {
        val groupToAssign = when (mode) {
            LoadSavedListMode.MERGE_CURRENT -> targetGroup
            LoadSavedListMode.ASSIGN_GROUP -> targetGroup
            LoadSavedListMode.REPLACE_NEW -> if (targetGroup.isBlank()) savedList.name else targetGroup
        }

        savedList.items.forEach { item ->
            val newItem = item.copy(
                id = UUID.randomUUID().toString(),
                isCompleted = false, // Always uncheck imported items so user can shop them fresh
                listGroup = groupToAssign
            )
            listRepository.addItem(newItem)
        }
    }
}
