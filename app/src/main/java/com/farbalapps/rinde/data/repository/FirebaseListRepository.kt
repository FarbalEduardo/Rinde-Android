package com.farbalapps.rinde.data.repository

import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.farbalapps.rinde.data.local.dao.ShoppingItemDao
import com.farbalapps.rinde.di.IoDispatcher
import com.farbalapps.rinde.data.local.mapper.toEntity
import com.farbalapps.rinde.data.local.mapper.toDomain
import com.farbalapps.rinde.data.worker.SyncShoppingItemWorker

import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ListRepository] using a robust Offline-First strategy:
 * - Room is the single source of truth for the UI.
 * - All write operations (add/update/delete) persist to Room immediately.
 * - A [SyncShoppingItemWorker] is enqueued to sync with Firebase Firestore in the
 *   background. WorkManager guarantees the sync runs even if the app is killed or
 *   the device is currently offline, with automatic retries.
 */
@Singleton
class FirebaseListRepository @Inject constructor(
    private val dao: ShoppingItemDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val workManager: WorkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
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
            Log.e(TAG, "Cannot add item: User NOT logged in.")
            throw Exception("User not logged in")
        }
        val itemWithId = if (item.id.isEmpty()) {
            item.copy(id = UUID.randomUUID().toString(), userId = userId)
        } else {
            item.copy(userId = userId)
        }

        withContext(ioDispatcher) {
            // 1. Write to Room immediately (UI reflects change at once)
            dao.insert(itemWithId.toEntity(userId))
            Log.d(TAG, "Room INSERT ok — id=${itemWithId.id}")

            // 2. Enqueue background sync to Firestore
            enqueueUpsert(userId, itemWithId)
        }
    }

    override suspend fun deleteItem(item: ShoppingItem) {
        val userId = currentUserId ?: return

        withContext(ioDispatcher) {
            // 1. Delete from Room immediately
            dao.delete(item.toEntity(userId))
            Log.d(TAG, "Room DELETE ok — id=${item.id}")

            // 2. Enqueue background sync to Firestore
            enqueueDelete(userId, item.id)
        }
    }

    override suspend fun updateItem(item: ShoppingItem) {
        val userId = currentUserId ?: return

        withContext(ioDispatcher) {
            // 1. Update Room immediately
            dao.update(item.toEntity(userId))
            Log.d(TAG, "Room UPDATE ok — id=${item.id}")

            // 2. Enqueue background sync to Firestore
            enqueueUpsert(userId, item)
        }
    }

    override suspend fun deleteItemsByGroup(group: String) {
        val userId = currentUserId ?: return

        withContext(ioDispatcher) {
            // 1. Delete from Room immediately
            dao.deleteByGroup(group)
            Log.d(TAG, "Room DELETE_BY_GROUP ok — group=$group")

            // 2. Enqueue background sync to Firestore
            enqueueDeleteGroup(userId, group)
        }
    }

    // -------------------------------------------------------------------------
    // WorkManager helpers
    // -------------------------------------------------------------------------

    private fun enqueueUpsert(userId: String, item: ShoppingItem) {
        val inputData = workDataOf(
            SyncShoppingItemWorker.KEY_OPERATION to SyncShoppingItemWorker.OPERATION_UPSERT,
            SyncShoppingItemWorker.KEY_USER_ID to userId,
            SyncShoppingItemWorker.KEY_ITEM_ID to item.id,
            SyncShoppingItemWorker.KEY_NAME to item.name,
            SyncShoppingItemWorker.KEY_CATEGORY to item.category,
            SyncShoppingItemWorker.KEY_IS_COMPLETED to item.isCompleted,
            SyncShoppingItemWorker.KEY_QUANTITY to item.quantity,
            SyncShoppingItemWorker.KEY_UNIT to item.unit,
            SyncShoppingItemWorker.KEY_EMOJI to item.emoji,
            SyncShoppingItemWorker.KEY_LIST_GROUP to item.listGroup
        )
        workManager.enqueue(
            OneTimeWorkRequestBuilder<SyncShoppingItemWorker>()
                .setInputData(inputData)
                .build()
        )
        Log.d(TAG, "Worker UPSERT enqueued — id=${item.id}")
    }

    private fun enqueueDelete(userId: String, itemId: String) {
        val inputData = workDataOf(
            SyncShoppingItemWorker.KEY_OPERATION to SyncShoppingItemWorker.OPERATION_DELETE,
            SyncShoppingItemWorker.KEY_USER_ID to userId,
            SyncShoppingItemWorker.KEY_ITEM_ID to itemId
        )
        workManager.enqueue(
            OneTimeWorkRequestBuilder<SyncShoppingItemWorker>()
                .setInputData(inputData)
                .build()
        )
        Log.d(TAG, "Worker DELETE enqueued — id=$itemId")
    }

    private fun enqueueDeleteGroup(userId: String, group: String) {
        val inputData = workDataOf(
            SyncShoppingItemWorker.KEY_OPERATION to SyncShoppingItemWorker.OPERATION_DELETE_GROUP,
            SyncShoppingItemWorker.KEY_USER_ID to userId,
            SyncShoppingItemWorker.KEY_LIST_GROUP to group
        )
        workManager.enqueue(
            OneTimeWorkRequestBuilder<SyncShoppingItemWorker>()
                .setInputData(inputData)
                .build()
        )
        Log.d(TAG, "Worker DELETE_GROUP enqueued — group=$group")
    }

    // -------------------------------------------------------------------------
    // Sync down (initial load from Firestore → Room)
    // -------------------------------------------------------------------------

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
            Log.d(TAG, "Firestore sync DOWN success: ${remoteItems.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync DOWN error", e)
        }
    }

    companion object {
        private const val TAG = "FirebaseListRepository"
    }
}

