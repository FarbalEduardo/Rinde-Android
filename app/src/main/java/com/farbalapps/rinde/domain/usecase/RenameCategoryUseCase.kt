package com.farbalapps.rinde.domain.usecase

import com.farbalapps.rinde.domain.repository.CategoryRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RenameCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val listRepository: ListRepository
) {
    suspend operator fun invoke(oldName: String, newName: String) {
        val currentCategories = categoryRepository.getCategories().first()
        val oldCategory = currentCategories.find { it.name == oldName } ?: return

        // 1. Create new category
        val newCategory = oldCategory.copy(name = newName)
        categoryRepository.addCategory(newCategory)

        // 2. Update all items in this group
        val items = listRepository.getItems().first().filter { it.listGroup == oldName }
        items.forEach { item ->
            listRepository.updateItem(item.copy(listGroup = newName))
        }

        // 3. Delete old category
        categoryRepository.deleteCategory(oldName)
    }
}
