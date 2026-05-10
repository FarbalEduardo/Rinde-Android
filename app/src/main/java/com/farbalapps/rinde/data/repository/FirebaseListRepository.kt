package com.farbalapps.rinde.data.repository

import android.util.Log
import com.farbalapps.rinde.data.local.dao.ShoppingItemDao
import com.farbalapps.rinde.data.local.mapper.toDomain
import com.farbalapps.rinde.data.local.mapper.toEntity
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ListRepository] using an Offline First strategy:
 * - Room is the single source of truth for the UI.
 * - Firebase Firestore is synced in the same suspend scope using [Task.await()].
 */
@Singleton
class FirebaseListRepository @Inject constructor(
    private val dao: ShoppingItemDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ListRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun userCollection(userId: String) =
        firestore.collection("users").document(userId).collection("shopping_items")

    override fun getItems(): Flow<List<ShoppingItem>> {
        val userId = currentUserId ?: return emptyFlow()
        return dao.getItemsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addItem(item: ShoppingItem) {
        val userId = currentUserId ?: run {
            Log.e("FirebaseListRepository", "Cannot add item: User NOT logged in.")
            throw Exception("User not logged in")
        }
        val itemWithId = if (item.id.isEmpty()) {
            item.copy(id = UUID.randomUUID().toString(), userId = userId)
        } else {
            item.copy(userId = userId)
        }

        withContext(Dispatchers.IO) {
            val docPath = "users/$userId/shopping_items/${itemWithId.id}"
            Log.d("FirebaseListRepository", "Writing to Room & Firestore. Path: $docPath")
            
            // 1. Write to local Room first (source of truth)
            dao.insert(itemWithId.toEntity(userId))

            // 2. Sync to Firestore
            try {
                userCollection(userId)
                    .document(itemWithId.id)
                    .set(itemWithId.toFirestoreMap())
                    .await()
                Log.d("FirebaseListRepository", "Firestore sync SUCCESS for $docPath")
            } catch (e: Exception) {
                Log.e("FirebaseListRepository", "Firestore sync ERROR for $docPath", e)
                throw e
            }
        }
    }

    override suspend fun deleteItem(item: ShoppingItem) {
        val userId = currentUserId ?: return

        withContext(Dispatchers.IO) {
            val docPath = "users/$userId/shopping_items/${item.id}"
            // 1. Delete from Room
            dao.delete(item.toEntity(userId))

            // 2. Delete from Firestore (awaited)
            try {
                userCollection(userId).document(item.id).delete().await()
                Log.d("FirebaseListRepository", "Firestore delete SUCCESS for $docPath")
            } catch (e: Exception) {
                Log.e("FirebaseListRepository", "Firestore delete ERROR for $docPath", e)
                throw e
            }
        }
    }

    override suspend fun updateItem(item: ShoppingItem) {
        val userId = currentUserId ?: return

        withContext(Dispatchers.IO) {
            val docPath = "users/$userId/shopping_items/${item.id}"
            // 1. Update Room
            dao.update(item.toEntity(userId))

            // 2. Update Firestore (awaited)
            try {
                userCollection(userId)
                    .document(item.id)
                    .set(item.toFirestoreMap())
                    .await()
                Log.d("FirebaseListRepository", "Firestore update SUCCESS for $docPath")
            } catch (e: Exception) {
                Log.e("FirebaseListRepository", "Firestore update ERROR for $docPath", e)
                throw e
            }
        }
    }

    override suspend fun deleteItemsByGroup(group: String) {
        val userId = currentUserId ?: return
        withContext(Dispatchers.IO) {
            // 1. Delete from Room
            dao.deleteByGroup(group)

            // 2. Delete from Firestore
            try {
                val snapshot = userCollection(userId)
                    .whereEqualTo("listGroup", group)
                    .get()
                    .await()
                
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
                Log.d("FirebaseListRepository", "Firestore delete by group SUCCESS for $group")
            } catch (e: Exception) {
                Log.e("FirebaseListRepository", "Firestore delete by group ERROR for $group", e)
                // Note: We don't throw here to avoid preventing local cleanup if Firestore fails
            }
        }
    }

    private fun ShoppingItem.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "category" to category,
        "isCompleted" to isCompleted,
        "quantity" to quantity,
        "unit" to unit,
        "emoji" to emoji,
        "listGroup" to listGroup,
        "userId" to userId
    )

    override suspend fun syncItems() {
        val userId = currentUserId ?: return
        try {
            val snapshot = userCollection(userId).get().await()
            val remoteItems = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val category = doc.getString("category") ?: ""
                val isCompleted = doc.getBoolean("isCompleted") ?: false
                val quantity = doc.getDouble("quantity") ?: 1.0
                val unit = doc.getString("unit") ?: "Pieza"
                val emoji = doc.getString("emoji") ?: ""
                val listGroup = doc.getString("listGroup") ?: "All"
                
                com.farbalapps.rinde.data.local.entity.ShoppingItemEntity(
                    id = id,
                    name = name,
                    category = category,
                    isCompleted = isCompleted,
                    quantity = quantity,
                    unit = unit,
                    emoji = emoji,
                    listGroup = listGroup,
                    userId = userId
                )
            }
            if (remoteItems.isNotEmpty()) {
                dao.insertAll(remoteItems)
            }
            Log.d("FirebaseListRepository", "Firestore sync DOWN success: ${remoteItems.size} items")
        } catch (e: Exception) {
            Log.e("FirebaseListRepository", "Firestore sync DOWN error", e)
        }
    }
}
