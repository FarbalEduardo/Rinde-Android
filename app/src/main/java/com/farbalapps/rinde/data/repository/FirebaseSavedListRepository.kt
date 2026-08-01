package com.farbalapps.rinde.data.repository

import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.farbalapps.rinde.data.local.dao.SavedListDao
import com.farbalapps.rinde.data.local.mapper.toDomain
import com.farbalapps.rinde.data.local.mapper.toEntity
import com.farbalapps.rinde.data.worker.SyncSavedListWorker
import com.farbalapps.rinde.di.IoDispatcher
import com.farbalapps.rinde.domain.model.SavedShoppingList
import com.farbalapps.rinde.domain.repository.SavedListRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSavedListRepository @Inject constructor(
    private val dao: SavedListDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val workManager: WorkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SavedListRepository {

    private val gson = Gson()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun savedListCollection(userId: String) =
        firestore.collection("users").document(userId).collection("saved_lists")

    override fun getSavedLists(): Flow<List<SavedShoppingList>> {
        val userId = currentUserId ?: return emptyFlow()
        return dao.getSavedListsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveList(savedList: SavedShoppingList) {
        val userId = currentUserId ?: run {
            Log.e(TAG, "Cannot save list: User NOT logged in.")
            return
        }
        val listWithUser = savedList.copy(userId = userId)

        withContext(ioDispatcher) {
            // 1. Save to Room immediately
            dao.insert(listWithUser.toEntity(userId))
            Log.d(TAG, "Room saved_lists INSERT ok — id=${listWithUser.id}")

            // 2. Enqueue Worker
            enqueueSaveWorker(userId, listWithUser)
        }
    }

    override suspend fun deleteSavedList(listId: String) {
        val userId = currentUserId ?: return

        withContext(ioDispatcher) {
            dao.deleteById(listId)
            Log.d(TAG, "Room saved_lists DELETE ok — id=$listId")

            enqueueDeleteWorker(userId, listId)
        }
    }

    override suspend fun renameSavedList(listId: String, newName: String) {
        val userId = currentUserId ?: return
        val now = System.currentTimeMillis()

        withContext(ioDispatcher) {
            dao.updateName(listId, newName, now)
            Log.d(TAG, "Room saved_lists RENAME ok — id=$listId, newName=$newName")

            enqueueRenameWorker(userId, listId, newName, now)
        }
    }

    override suspend fun syncSavedLists() {
        val userId = currentUserId ?: return
        try {
            val snapshot = savedListCollection(userId).get().await()
            val remoteLists = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val savedAt = doc.getLong("savedAt") ?: System.currentTimeMillis()
                val lastModifiedAt = doc.getLong("lastModifiedAt") ?: savedAt
                val totalItems = (doc.getLong("totalItems") ?: 0L).toInt()
                val completedItems = (doc.getLong("completedItems") ?: 0L).toInt()
                val totalPrice = doc.getDouble("totalPrice")
                val currency = doc.getString("currency") ?: "MXN"

                @Suppress("UNCHECKED_CAST")
                val rawItems = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                val itemsJson = gson.toJson(rawItems)
                val type = object : com.google.gson.reflect.TypeToken<List<com.farbalapps.rinde.domain.model.ShoppingItem>>() {}.type
                val items: List<com.farbalapps.rinde.domain.model.ShoppingItem> = gson.fromJson(itemsJson, type) ?: emptyList()

                com.farbalapps.rinde.data.local.entity.SavedListEntity(
                    id = id,
                    name = name,
                    savedAt = savedAt,
                    lastModifiedAt = lastModifiedAt,
                    totalItems = totalItems,
                    completedItems = completedItems,
                    totalPrice = totalPrice,
                    currency = currency,
                    items = items,
                    userId = userId
                )
            }
            if (remoteLists.isNotEmpty()) {
                dao.insertAll(remoteLists)
            }
            Log.d(TAG, "Firestore saved_lists sync DOWN success: ${remoteLists.size} lists")
        } catch (e: Exception) {
            Log.e(TAG, "Firestore saved_lists sync DOWN error", e)
        }
    }

    private fun enqueueSaveWorker(userId: String, savedList: SavedShoppingList) {
        val itemsJson = gson.toJson(savedList.items)
        val builder = mutableMapOf<String, Any>(
            SyncSavedListWorker.KEY_OPERATION to SyncSavedListWorker.OPERATION_SAVE,
            SyncSavedListWorker.KEY_USER_ID to userId,
            SyncSavedListWorker.KEY_LIST_ID to savedList.id,
            SyncSavedListWorker.KEY_NAME to savedList.name,
            SyncSavedListWorker.KEY_SAVED_AT to savedList.savedAt,
            SyncSavedListWorker.KEY_LAST_MODIFIED_AT to savedList.lastModifiedAt,
            SyncSavedListWorker.KEY_TOTAL_ITEMS to savedList.totalItems,
            SyncSavedListWorker.KEY_COMPLETED_ITEMS to savedList.completedItems,
            SyncSavedListWorker.KEY_CURRENCY to savedList.currency,
            SyncSavedListWorker.KEY_ITEMS_JSON to itemsJson
        )
        savedList.totalPrice?.let { builder[SyncSavedListWorker.KEY_TOTAL_PRICE] = it }

        val inputData = workDataOf(*builder.toList().toTypedArray())
        workManager.enqueue(
            OneTimeWorkRequestBuilder<SyncSavedListWorker>()
                .setInputData(inputData)
                .build()
        )
    }

    private fun enqueueDeleteWorker(userId: String, listId: String) {
        val inputData = workDataOf(
            SyncSavedListWorker.KEY_OPERATION to SyncSavedListWorker.OPERATION_DELETE,
            SyncSavedListWorker.KEY_USER_ID to userId,
            SyncSavedListWorker.KEY_LIST_ID to listId
        )
        workManager.enqueue(
            OneTimeWorkRequestBuilder<SyncSavedListWorker>()
                .setInputData(inputData)
                .build()
        )
    }

    private fun enqueueRenameWorker(userId: String, listId: String, newName: String, updatedAt: Long) {
        val inputData = workDataOf(
            SyncSavedListWorker.KEY_OPERATION to SyncSavedListWorker.OPERATION_RENAME,
            SyncSavedListWorker.KEY_USER_ID to userId,
            SyncSavedListWorker.KEY_LIST_ID to listId,
            SyncSavedListWorker.KEY_NAME to newName,
            SyncSavedListWorker.KEY_LAST_MODIFIED_AT to updatedAt
        )
        workManager.enqueue(
            OneTimeWorkRequestBuilder<SyncSavedListWorker>()
                .setInputData(inputData)
                .build()
        )
    }

    companion object {
        private const val TAG = "FirebaseSavedListRepository"
    }
}
