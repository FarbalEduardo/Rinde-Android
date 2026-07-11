package com.farbalapps.rinde.data.repository.delegate

import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.UserVoteDao
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.Profile
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProfileSocialDelegate @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val postDao: PostDao,
    private val userVoteDao: UserVoteDao
) {
    fun getProfilePosts(userId: String): Flow<List<CommunityPost>> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = firestore.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .whereEqualTo("isActive", true)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
                }
                
                if (posts.isNotEmpty()) {
                    val enriched = posts.map { post ->
                        val localVote = userVoteDao.getVoteOnce(post.id, userId)?.voteValue ?: 0
                        post.copy(myVoteValue = localVote).toEntity()
                    }
                    postDao.upsertPosts(enriched)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileSocialDelegate", "Error performing initial fetch of profile posts: ${e.message}")
            }
        }

        return postDao.getPostsByAuthorId(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun toggleSavePost(userId: String, postId: String, save: Boolean): Result<Unit> = runCatching {
        val savedRef = firestore.collection("users").document(userId)
            .collection("saved_posts").document(postId)

        if (save) {
            savedRef.set(mapOf(
                "postId" to postId,
                "savedAt" to FieldValue.serverTimestamp()
            )).await()
        } else {
            savedRef.delete().await()
        }
    }

    suspend fun blockUser(userId: String, targetUserId: String): Result<Unit> = runCatching {
        val blockRef = firestore.collection("users").document(userId)
            .collection("blocked_users").document(targetUserId)
        
        firestore.runTransaction { transaction ->
            transaction.set(blockRef, mapOf(
                "blockedId" to targetUserId,
                "blockedAt" to FieldValue.serverTimestamp()
            ))
        }.await()
    }

    suspend fun unblockUser(userId: String, targetUserId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(userId)
            .collection("blocked_users").document(targetUserId)
            .delete()
            .await()
    }

    fun getSavedProfilePosts(userId: String): Flow<List<CommunityPost>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("saved_posts")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val postIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (postIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    try {
                        val posts = postIds.chunked(30).flatMap { chunk ->
                            firestore.collection("posts")
                                .whereIn(FieldPath.documentId(), chunk)
                                .whereEqualTo("isActive", true)
                                .get().await()
                                .documents.mapNotNull { doc ->
                                    doc.toObject(CommunityPostDto::class.java)
                                        ?.copy(id = doc.id)
                                        ?.toDomain()
                                }
                        }
                        val ordered = postIds.mapNotNull { id -> posts.find { it.id == id } }
                        trySend(ordered)
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileSocialDelegate", "❌ Error hydrating saved posts", e)
                        trySend(emptyList())
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    fun getBlockedUsers(userId: String): Flow<List<Profile>> = callbackFlow {
        val listener = firestore.collection("users").document(userId).collection("blocked_users")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val blockedIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (blockedIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                firestore.collection("users")
                    .whereIn(FieldPath.documentId(), blockedIds.take(30))
                    .get()
                    .addOnSuccessListener { userSnapshot ->
                        val users = userSnapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            Profile(
                                id = doc.id,
                                name = data["name"] as? String ?: "Usuario",
                                photoUrl = data["photoUrl"] as? String
                            )
                        }
                        trySend(users)
                    }
                    .addOnFailureListener {
                        trySend(emptyList())
                    }
            }
        awaitClose { listener.remove() }
    }
}
