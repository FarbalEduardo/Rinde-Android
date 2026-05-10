package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.local.dao.CategoryDao
import com.farbalapps.rinde.data.local.entity.CategoryEntity
import com.farbalapps.rinde.domain.model.Category
import com.farbalapps.rinde.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.emptyFlow

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CategoryRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun userGroupsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("shopping_groups")

    override fun getCategories(): Flow<List<Category>> {
        val userId = currentUserId ?: return emptyFlow()
        return categoryDao.getCategoriesByUser(userId).map { entities ->
            entities.map { Category(it.name, it.userId, it.isCustom, it.orderIndex) }
        }
    }

    override suspend fun addCategory(category: Category) {
        val userId = currentUserId ?: return
        val categoryWithUser = category.copy(userId = userId)
        
        // 1. Local
        categoryDao.insert(CategoryEntity(categoryWithUser.name, userId, categoryWithUser.isCustom, categoryWithUser.orderIndex))
        
        // 2. Remote
        try {
            userGroupsCollection(userId)
                .document(categoryWithUser.name)
                .set(mapOf(
                    "name" to categoryWithUser.name,
                    "isCustom" to categoryWithUser.isCustom,
                    "orderIndex" to categoryWithUser.orderIndex
                ))
                .await()
        } catch (e: Exception) {
            // Log error but keep local
        }
    }

    override suspend fun deleteCategory(name: String) {
        val userId = currentUserId ?: return
        
        // 1. Local
        categoryDao.deleteByName(name, userId)
        
        // 2. Remote
        try {
            userGroupsCollection(userId).document(name).delete().await()
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun syncCategories() {
        val userId = currentUserId ?: return
        try {
            val snapshot = userGroupsCollection(userId).get().await()
            val remoteCategories = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val isCustom = doc.getBoolean("isCustom") ?: true
                val orderIndex = doc.getLong("orderIndex")?.toInt() ?: 0
                CategoryEntity(name, userId, isCustom, orderIndex)
            }
            if (remoteCategories.isNotEmpty()) {
                categoryDao.insertAll(remoteCategories)
            }
        } catch (e: Exception) {
            // Log error
        }
    }
}
