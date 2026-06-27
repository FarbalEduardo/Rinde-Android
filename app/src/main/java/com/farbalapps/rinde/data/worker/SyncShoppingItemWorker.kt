package com.farbalapps.rinde.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * WorkManager worker that synchronizes shopping item CRUD operations with Firebase Firestore.
 *
 * This worker is enqueued after local Room writes succeed, guaranteeing that:
 * - The UI always reflects local state immediately (Room = single source of truth).
 * - Remote sync is retried automatically if the device is offline or the app is killed.
 *
 * Supported operations (set via [KEY_OPERATION] input data):
 * - [OPERATION_UPSERT]: Creates or updates a Firestore document for the item.
 * - [OPERATION_DELETE]: Deletes a Firestore document by item ID.
 * - [OPERATION_DELETE_GROUP]: Deletes all Firestore documents matching a listGroup.
 */
@HiltWorker
class SyncShoppingItemWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val operation = inputData.getString(KEY_OPERATION) ?: return Result.failure()
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()

        return try {
            when (operation) {
                OPERATION_UPSERT -> handleUpsert(userId)
                OPERATION_DELETE -> handleDelete(userId)
                OPERATION_DELETE_GROUP -> handleDeleteGroup(userId)
                else -> {
                    Log.e(TAG, "Unknown operation: $operation")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing operation=$operation, attempt=$runAttemptCount", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    // -------------------------------------------------------------------------
    // Private handlers
    // -------------------------------------------------------------------------

    private suspend fun handleUpsert(userId: String): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()

        val firestoreMap = buildItemMap(userId, itemId) ?: return Result.failure()
        val docPath = "users/$userId/shopping_items/$itemId"

        userCollection(userId)
            .document(itemId)
            .set(firestoreMap)
            .await()

        Log.d(TAG, "✅ UPSERT success — $docPath")
        return Result.success()
    }

    private suspend fun handleDelete(userId: String): Result {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return Result.failure()

        userCollection(userId).document(itemId).delete().await()
        Log.d(TAG, "✅ DELETE success — users/$userId/shopping_items/$itemId")
        return Result.success()
    }

    private suspend fun handleDeleteGroup(userId: String): Result {
        val group = inputData.getString(KEY_LIST_GROUP) ?: return Result.failure()

        val snapshot = userCollection(userId)
            .whereEqualTo("listGroup", group)
            .get()
            .await()

        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }

        Log.d(TAG, "✅ DELETE_GROUP success — group=$group, deleted=${snapshot.size()}")
        return Result.success()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun userCollection(userId: String) =
        firestore.collection("users").document(userId).collection("shopping_items")

    /** Builds the Firestore map from input data fields. Returns null if required fields are missing. */
    private fun buildItemMap(userId: String, itemId: String): Map<String, Any>? {
        val name = inputData.getString(KEY_NAME) ?: return null
        val category = inputData.getString(KEY_CATEGORY) ?: return null
        val isCompleted = inputData.getBoolean(KEY_IS_COMPLETED, false)
        val quantity = inputData.getDouble(KEY_QUANTITY, 1.0)
        val unit = inputData.getString(KEY_UNIT) ?: "Pieza"
        val emoji = inputData.getString(KEY_EMOJI) ?: ""
        val listGroup = inputData.getString(KEY_LIST_GROUP) ?: "All"

        return mapOf(
            "id" to itemId,
            "name" to name,
            "category" to category,
            "isCompleted" to isCompleted,
            "quantity" to quantity,
            "unit" to unit,
            "emoji" to emoji,
            "listGroup" to listGroup,
            "userId" to userId
        )
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    companion object {
        private const val TAG = "SyncShoppingItemWorker"
        private const val MAX_RETRIES = 3

        // Input data keys
        const val KEY_OPERATION = "operation"
        const val KEY_USER_ID = "userId"
        const val KEY_ITEM_ID = "itemId"
        const val KEY_NAME = "name"
        const val KEY_CATEGORY = "category"
        const val KEY_IS_COMPLETED = "isCompleted"
        const val KEY_QUANTITY = "quantity"
        const val KEY_UNIT = "unit"
        const val KEY_EMOJI = "emoji"
        const val KEY_LIST_GROUP = "listGroup"

        // Operation types
        const val OPERATION_UPSERT = "UPSERT"
        const val OPERATION_DELETE = "DELETE"
        const val OPERATION_DELETE_GROUP = "DELETE_GROUP"
    }
}
