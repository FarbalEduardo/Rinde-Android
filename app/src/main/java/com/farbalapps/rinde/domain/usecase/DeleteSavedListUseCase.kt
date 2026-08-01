package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.SavedListRepository
import javax.inject.Inject

/**
 * Use case to delete a saved list backup.
 */
class DeleteSavedListUseCase @Inject constructor(
    private val repository: SavedListRepository
) {
    suspend operator fun invoke(listId: String) = repository.deleteSavedList(listId)
}
