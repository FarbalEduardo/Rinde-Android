package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.SavedShoppingList
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.SavedListRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Use case to save a snapshot of current shopping items as a backup list.
 */
class SaveCurrentListUseCase @Inject constructor(
    private val savedListRepository: SavedListRepository
) {
    suspend operator fun invoke(name: String, items: List<ShoppingItem>) {
        val totalItems = items.size
        val completedItems = items.count { it.isCompleted }
        val itemsWithPrice = items.filter { it.price != null }
        val totalPrice = if (itemsWithPrice.isNotEmpty()) itemsWithPrice.sumOf { (it.price ?: 0.0) * it.quantity } else null
        val currency = items.firstOrNull { it.price != null }?.currency ?: "MXN"
        val now = System.currentTimeMillis()

        val savedList = SavedShoppingList(
            id = UUID.randomUUID().toString(),
            name = name,
            savedAt = now,
            lastModifiedAt = now,
            totalItems = totalItems,
            completedItems = completedItems,
            totalPrice = totalPrice,
            currency = currency,
            items = items
        )
        savedListRepository.saveList(savedList)
    }
}
