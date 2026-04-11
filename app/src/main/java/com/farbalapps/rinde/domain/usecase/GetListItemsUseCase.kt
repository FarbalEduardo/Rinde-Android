package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case that retrieves the observable list of shopping items for the current user.
 */
class GetListItemsUseCase @Inject constructor(
    private val repository: ListRepository
) {
    operator fun invoke(): Flow<List<ShoppingItem>> = repository.getItems()
}
