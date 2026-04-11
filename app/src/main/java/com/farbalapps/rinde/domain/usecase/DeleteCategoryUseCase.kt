package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.CategoryRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val listRepository: ListRepository
) {
    suspend operator fun invoke(name: String) {
        // 1. Delete the category record
        categoryRepository.deleteCategory(name)
        
        // 2. Delete all items belonging to this group
        listRepository.deleteItemsByGroup(name)
    }
}
