package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import javax.inject.Inject

/**
 * Use case that adds a new shopping item.
 */
class AddListItemUseCase @Inject constructor(
    private val repository: ListRepository
) {
    suspend operator fun invoke(item: ShoppingItem) = repository.addItem(item)
}
