package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import javax.inject.Inject

/**
 * Use case that toggles the completion status of a shopping item.
 */
class ToggleListItemUseCase @Inject constructor(
    private val repository: ListRepository
) {
    suspend operator fun invoke(item: ShoppingItem, isCompleted: Boolean) {
        repository.updateItem(item.copy(isCompleted = isCompleted))
    }
}
