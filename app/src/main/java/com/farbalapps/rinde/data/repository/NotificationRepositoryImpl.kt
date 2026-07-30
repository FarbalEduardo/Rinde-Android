package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.di.IoDispatcher
import com.farbalapps.rinde.domain.model.AppNotification
import com.farbalapps.rinde.domain.model.NotificationType
import com.farbalapps.rinde.domain.repository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

import kotlinx.coroutines.tasks.await

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NotificationRepository {

    override fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("notifications")
            .document(userId)
            .collection("items")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun getNotifications(userId: String): Flow<List<AppNotification>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = firestore.collection("notifications")
            .document(userId)
            .collection("items")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val typeStr = doc.getString("type") ?: NotificationType.NEW_COMMENT.name
                        val type = try {
                            NotificationType.valueOf(typeStr)
                        } catch (e: Exception) {
                            NotificationType.NEW_COMMENT
                        }
                        val postId = doc.getString("postId") ?: ""
                        val postTitle = doc.getString("postTitle") ?: ""
                        val actorName = doc.getString("actorName")
                        val actorPhotoUrl = doc.getString("actorPhotoUrl")
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val isRead = doc.getBoolean("isRead") ?: false

                        AppNotification(
                            id = id,
                            type = type,
                            postId = postId,
                            postTitle = postTitle,
                            actorName = actorName,
                            actorPhotoUrl = actorPhotoUrl,
                            timestamp = timestamp,
                            isRead = isRead
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(list)
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun sendNotification(
        targetUserId: String,
        notification: AppNotification
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (targetUserId.isEmpty()) return@withContext Result.success(Unit)

            val notifId = if (notification.id.isNotEmpty()) notification.id else UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to notifId,
                "type" to notification.type.name,
                "postId" to notification.postId,
                "postTitle" to notification.postTitle,
                "actorName" to notification.actorName,
                "actorPhotoUrl" to notification.actorPhotoUrl,
                "timestamp" to notification.timestamp,
                "isRead" to false
            )

            firestore.collection("notifications")
                .document(targetUserId)
                .collection("items")
                .document(notifId)
                .set(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(userId: String, notificationId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (userId.isEmpty() || notificationId.isEmpty()) return@withContext Result.success(Unit)

            firestore.collection("notifications")
                .document(userId)
                .collection("items")
                .document(notificationId)
                .update("isRead", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (userId.isEmpty()) return@withContext Result.success(Unit)

            val snapshot = firestore.collection("notifications")
                .document(userId)
                .collection("items")
                .whereEqualTo("isRead", false)
                .get()
                .await()

            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
