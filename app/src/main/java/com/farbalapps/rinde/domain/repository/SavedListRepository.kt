package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.SavedShoppingList
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for saved shopping list backups/snapshots.
 */
interface SavedListRepository {

    /**
     * Returns a Flow observing saved lists for the current user.
     */
    fun getSavedLists(): Flow<List<SavedShoppingList>>

    /**
     * Saves a snapshot of a shopping list to local DB and syncs to Firebase.
     */
    suspend fun saveList(savedList: SavedShoppingList)

    /**
     * Deletes a saved list backup by ID.
     */
    suspend fun deleteSavedList(listId: String)

    /**
     * Renames a saved list.
     */
    suspend fun renameSavedList(listId: String, newName: String)

    /**
     * Triggers sync of saved lists from Firestore to Room.
     */
    suspend fun syncSavedLists()
}
