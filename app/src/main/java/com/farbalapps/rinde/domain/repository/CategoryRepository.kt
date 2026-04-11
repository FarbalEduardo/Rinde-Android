package com.farbalapps.rinde.domain.repository

import com.farbalapps.rinde.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category)
    suspend fun deleteCategory(name: String)
    suspend fun syncCategories()
}
