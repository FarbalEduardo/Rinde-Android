package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.SavedListRepository
import javax.inject.Inject

/**
 * Use case to rename a saved shopping list backup.
 */
class RenameSavedListUseCase @Inject constructor(
    private val repository: SavedListRepository
) {
    suspend operator fun invoke(listId: String, newName: String) =
        repository.renameSavedList(listId, newName)
}
