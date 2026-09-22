package com.farbalapps.rinde.data.repository

import com.farbalapps.rinde.data.local.dao.ChatDao
import com.farbalapps.rinde.data.local.entity.toDomain
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.domain.model.ChatConversation
import com.farbalapps.rinde.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChatRepository connecting Room local persistence and Firestore sync.
 * Guarantees maximum 10 saved conversation sessions per user.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dao: ChatDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @com.farbalapps.rinde.di.IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ChatRepository {

    override fun getConversations(userId: String): Flow<List<ChatConversation>> {
        return dao.getConversationsFlow(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConversationById(id: String): ChatConversation? = withContext(ioDispatcher) {
        dao.getConversationById(id)?.toDomain()
    }

    override suspend fun saveConversation(conversation: ChatConversation): Result<Unit> = withContext(ioDispatcher) {
        try {
            val userId = conversation.userId.ifEmpty { auth.currentUser?.uid.orEmpty() }
            if (userId.isEmpty()) return@withContext Result.failure(Exception("Usuario no autenticado"))

            val updatedConversation = conversation.copy(userId = userId, updatedAt = System.currentTimeMillis())

            // 1. Save to Room local DB
            dao.insertOrUpdate(updatedConversation.toEntity())
            dao.trimOldConversations(userId)

            // 2. Save to Firestore under /users/{userId}/chat_history/{chatId}
            firestore.collection("users")
                .document(userId)
                .collection("chat_history")
                .document(updatedConversation.id)
                .set(updatedConversation)
                .await()

            // 3. Trim Firestore old conversations beyond 10
            trimFirestoreOldConversations(userId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteConversation(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val userId = auth.currentUser?.uid.orEmpty()
            dao.deleteById(id)
            if (userId.isNotEmpty()) {
                firestore.collection("users")
                    .document(userId)
                    .collection("chat_history")
                    .document(id)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun trimFirestoreOldConversations(userId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("chat_history")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            if (snapshot.documents.size > 10) {
                val toDelete = snapshot.documents.drop(10)
                for (doc in toDelete) {
                    doc.reference.delete().await()
                }
            }
        } catch (_: Exception) { }
    }
}
