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
            // Write to Room immediately (Local storage only for active/draft list)
            dao.insert(itemWithId.toEntity(userId))
            Log.d(TAG, "Room INSERT ok (Local Only) — id=${itemWithId.id}")
        }
    }

    override suspend fun deleteItem(item: ShoppingItem) {
        val userId = currentUserId ?: return

        withContext(ioDispatcher) {
            // Delete from Room immediately
            dao.delete(item.toEntity(userId))
            Log.d(TAG, "Room DELETE ok (Local Only) — id=${item.id}")
        }
    }

    override suspend fun updateItem(item: ShoppingItem) {
        val userId = currentUserId ?: return

        withContext(ioDispatcher) {
            // Update Room immediately
            dao.update(item.toEntity(userId))
            Log.d(TAG, "Room UPDATE ok (Local Only) — id=${item.id}")
        }
    }

    override suspend fun deleteItems(items: List<ShoppingItem>) {
        val userId = currentUserId ?: return
        if (items.isEmpty()) return

        withContext(ioDispatcher) {
            val entities = items.map { it.toEntity(userId) }
            dao.deleteAll(entities)
            Log.d(TAG, "Room DELETE_BATCH ok (Local Only) — count=${items.size}")
        }
    }

    override suspend fun deleteItemsByGroup(group: String) {
        val userId = currentUserId ?: return

        withContext(ioDispatcher) {
            // Delete from Room immediately
            dao.deleteByGroup(group)
            Log.d(TAG, "Room DELETE_BY_GROUP ok (Local Only) — group=$group")
        }
    }

    override suspend fun syncItems() {
        // Active/draft items remain local in Room; only saved lists are synced via FirebaseSavedListRepository
        Log.d(TAG, "syncItems: Active list items remain local to Room.")
    }

    companion object {
        private const val TAG = "FirebaseListRepository"
    }
}

