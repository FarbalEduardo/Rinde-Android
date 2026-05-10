package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.ListRepository
import javax.inject.Inject

class SyncItemsUseCase @Inject constructor(
    private val repository: ListRepository
) {
    suspend operator fun invoke() {
        repository.syncItems()
    }
}
