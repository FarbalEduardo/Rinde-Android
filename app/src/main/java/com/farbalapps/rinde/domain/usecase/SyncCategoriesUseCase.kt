package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.CategoryRepository
import javax.inject.Inject

class SyncCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke() {
        repository.syncCategories()
    }
}
