package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import javax.inject.Inject

/**
 * Use case to delete multiple shopping items in batch.
 */
class DeleteMultipleItemsUseCase @Inject constructor(
    private val repository: ListRepository
) {
    suspend operator fun invoke(items: List<ShoppingItem>) = repository.deleteItems(items)
}
