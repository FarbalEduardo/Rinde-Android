package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.Category
import com.farbalapps.rinde.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.getCategories()
    }
}
