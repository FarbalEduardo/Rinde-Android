package com.farbalapps.rinde.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * WorkManager worker that synchronizes saved list CRUD operations with Firebase Firestore.
 */
@HiltWorker
class SyncSavedListWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        val operation = inputData.getString(KEY_OPERATION) ?: return Result.failure()
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val listId = inputData.getString(KEY_LIST_ID) ?: return Result.failure()

        return try {
            when (operation) {
                OPERATION_SAVE -> handleSave(userId, listId)
                OPERATION_DELETE -> handleDelete(userId, listId)
                OPERATION_RENAME -> handleRename(userId, listId)
                else -> {
                    Log.e(TAG, "Unknown operation: $operation")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing saved list operation=$operation, attempt=$runAttemptCount", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private suspend fun handleSave(userId: String, listId: String): Result {
        val name = inputData.getString(KEY_NAME) ?: return Result.failure()
        val savedAt = inputData.getLong(KEY_SAVED_AT, System.currentTimeMillis())
        val lastModifiedAt = inputData.getLong(KEY_LAST_MODIFIED_AT, savedAt)
        val totalItems = inputData.getInt(KEY_TOTAL_ITEMS, 0)
        val completedItems = inputData.getInt(KEY_COMPLETED_ITEMS, 0)
        val totalPrice = if (inputData.keyValueMap.containsKey(KEY_TOTAL_PRICE)) {
            inputData.getDouble(KEY_TOTAL_PRICE, 0.0)
        } else null
        val currency = inputData.getString(KEY_CURRENCY) ?: "MXN"
        val itemsJson = inputData.getString(KEY_ITEMS_JSON) ?: "[]"

        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val itemsMap: List<Map<String, Any>> = gson.fromJson(itemsJson, type) ?: emptyList()

        val firestoreMap = mutableMapOf<String, Any>(
            "id" to listId,
            "name" to name,
            "savedAt" to savedAt,
            "lastModifiedAt" to lastModifiedAt,
            "totalItems" to totalItems,
            "completedItems" to completedItems,
            "currency" to currency,
            "items" to itemsMap,
            "userId" to userId
        )
        totalPrice?.let { firestoreMap["totalPrice"] = it }

        savedListCollection(userId).document(listId).set(firestoreMap).await()
        Log.d(TAG, "✅ SAVE success — users/$userId/saved_lists/$listId")
        return Result.success()
    }

    private suspend fun handleDelete(userId: String, listId: String): Result {
        savedListCollection(userId).document(listId).delete().await()
        Log.d(TAG, "✅ DELETE success — users/$userId/saved_lists/$listId")
        return Result.success()
    }

    private suspend fun handleRename(userId: String, listId: String): Result {
        val newName = inputData.getString(KEY_NAME) ?: return Result.failure()
        val updatedAt = inputData.getLong(KEY_LAST_MODIFIED_AT, System.currentTimeMillis())

        savedListCollection(userId).document(listId).update(
            mapOf(
                "name" to newName,
                "lastModifiedAt" to updatedAt
            )
        ).await()
        Log.d(TAG, "✅ RENAME success — users/$userId/saved_lists/$listId")
        return Result.success()
    }

    private fun savedListCollection(userId: String) =
        firestore.collection("users").document(userId).collection("saved_lists")

    companion object {
        private const val TAG = "SyncSavedListWorker"
        private const val MAX_RETRIES = 3

        const val KEY_OPERATION = "operation"
        const val KEY_USER_ID = "userId"
        const val KEY_LIST_ID = "listId"
        const val KEY_NAME = "name"
        const val KEY_SAVED_AT = "savedAt"
        const val KEY_LAST_MODIFIED_AT = "lastModifiedAt"
        const val KEY_TOTAL_ITEMS = "totalItems"
        const val KEY_COMPLETED_ITEMS = "completedItems"
        const val KEY_TOTAL_PRICE = "totalPrice"
        const val KEY_CURRENCY = "currency"
        const val KEY_ITEMS_JSON = "itemsJson"

        const val OPERATION_SAVE = "SAVE"
        const val OPERATION_DELETE = "DELETE"
        const val OPERATION_RENAME = "RENAME"
    }
}
