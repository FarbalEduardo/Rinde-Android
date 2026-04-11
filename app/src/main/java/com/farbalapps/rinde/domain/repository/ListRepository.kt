package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for shopping list operations.
 * Follows the Repository Pattern from Clean Architecture.
 */
interface ListRepository {

    /**
     * Returns a Flow of shopping items for the currently authenticated user.
     */
    fun getItems(): Flow<List<ShoppingItem>>

    /**
     * Adds a new shopping item to local DB and syncs to Firebase.
     */
    suspend fun addItem(item: ShoppingItem)

    /**
     * Deletes a shopping item from local DB and Firebase.
     */
    suspend fun deleteItem(item: ShoppingItem)

    /**
     * Updates an existing shopping item (e.g., toggle completion).
     */
    suspend fun updateItem(item: ShoppingItem)

    /**
     * Deletes all items belonging to a specific shopping group.
     */
    suspend fun deleteItemsByGroup(group: String)

    suspend fun syncItems()
}
