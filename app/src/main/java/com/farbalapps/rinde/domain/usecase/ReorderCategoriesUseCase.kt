package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.model.Category
import com.farbalapps.rinde.domain.repository.CategoryRepository
import javax.inject.Inject

class ReorderCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(orderedCategories: List<Category>) {
        orderedCategories.forEachIndexed { index, category ->
            if (category.orderIndex != index) {
                categoryRepository.addCategory(category.copy(orderIndex = index))
            }
        }
    }
}
