package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.SavedListRepository
import javax.inject.Inject

/**
 * Use case to sync saved shopping lists from Firestore.
 */
class SyncSavedListsUseCase @Inject constructor(
    private val repository: SavedListRepository
) {
    suspend operator fun invoke() = repository.syncSavedLists()
}
