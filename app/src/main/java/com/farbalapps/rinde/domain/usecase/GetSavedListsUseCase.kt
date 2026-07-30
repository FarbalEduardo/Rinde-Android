package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.SavedShoppingList
import com.farbalapps.rinde.domain.repository.SavedListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe saved shopping list backups.
 */
class GetSavedListsUseCase @Inject constructor(
    private val repository: SavedListRepository
) {
    operator fun invoke(): Flow<List<SavedShoppingList>> = repository.getSavedLists()
}
