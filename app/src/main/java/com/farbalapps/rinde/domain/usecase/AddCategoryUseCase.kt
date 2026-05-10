package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.Category
import com.farbalapps.rinde.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(categoryName: String) {
        if (categoryName.isNotBlank()) {
            repository.addCategory(Category(categoryName, isCustom = true))
        }
    }
}
