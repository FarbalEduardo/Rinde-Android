package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import javax.inject.Inject

/**
 * Use case that deletes a shopping item.
 */
class DeleteListItemUseCase @Inject constructor(
    private val repository: ListRepository
) {
    suspend operator fun invoke(item: ShoppingItem) = repository.deleteItem(item)
}
