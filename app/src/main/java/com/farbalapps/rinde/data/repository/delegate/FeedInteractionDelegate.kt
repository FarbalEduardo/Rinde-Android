package com.farbalapps.rinde.data.repository.delegate

import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.UserVoteDao
import com.farbalapps.rinde.data.local.entity.UserVoteEntity
import com.farbalapps.rinde.data.util.SavedPostsMemoryCache
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.VoteOverlay
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject

class FeedInteractionDelegate @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val postDao: PostDao,
    private val userVoteDao: UserVoteDao,
    private val savedPostsMemoryCache: SavedPostsMemoryCache
) {
    companion object {
        private const val TAG = "FeedInteractionDelegate"
        private const val FEED_LIMIT = 30L
    }

    private val _globalPostStatus = MutableStateFlow<Map<String, VerificationStatus>>(emptyMap())
    val globalPostStatus = _globalPostStatus.asStateFlow()

    private val _globalSavedStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val globalSavedStatus = _globalSavedStatus.asStateFlow()

    private val _globalVoteStatus = MutableStateFlow<Map<String, VoteOverlay>>(emptyMap())
    val globalVoteStatus = _globalVoteStatus.asStateFlow()

    fun updatePostStatusLocal(postId: String, status: VerificationStatus) {
        _globalPostStatus.update { it + (postId to status) }
    }

    fun updateSavedStatusLocal(postId: String, isSaved: Boolean) {
        _globalSavedStatus.update { it + (postId to isSaved) }
    }

    fun updateVoteStatusLocal(postId: String, overlay: VoteOverlay) {
        _globalVoteStatus.update { it + (postId to overlay) }
    }

    fun clearSessionState() {
        _globalPostStatus.update { emptyMap() }
        _globalSavedStatus.update { emptyMap() }
        _globalVoteStatus.update { emptyMap() }
    }

    fun getSavedPosts(
        userId: String,
        snapshotToPosts: suspend (com.google.firebase.firestore.QuerySnapshot?) -> List<CommunityPost>
    ): Flow<List<CommunityPost>> = callbackFlow {
        android.util.Log.d(TAG, "🔖 Iniciando Saved Posts para user: $userId")

        var postsListener: com.google.firebase.firestore.ListenerRegistration? = null

        val savedListener = firestore.collection("users").document(userId)
            .collection("saved_posts")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .limit(FEED_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val postIds = snapshot?.documents?.map { it.id } ?: emptyList()
                if (postIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                postsListener?.remove()

                postsListener = firestore.collection("posts")
                    .whereEqualTo("isActive", true)
                    .whereIn(FieldPath.documentId(), postIds.take(30))
                    .addSnapshotListener { postSnapshot, postError ->
                        if (postError != null) {
                            android.util.Log.e(TAG, "❌ Error en Saved Posts posts: ${postError.message}")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        launch {
                            val posts = snapshotToPosts(postSnapshot)
                                .map { it.copy(isSavedByMe = true) }
                                .sortedBy { postIds.indexOf(it.id) }
                            trySend(posts)
                        }
                    }
            }

        awaitClose {
            savedListener.remove()
            postsListener?.remove()
        }
    }

    suspend fun toggleLike(userId: String, postId: String): Result<Unit> = runCatching {
        val likeRef = firestore.collection("posts").document(postId).collection("likes").document(userId)
        val postRef = firestore.collection("posts").document(postId)
        firestore.runTransaction { transaction ->
            val likeSnapshot = transaction.get(likeRef)
            if (likeSnapshot.exists()) {
                transaction.delete(likeRef)
                transaction.update(postRef, "likes", FieldValue.increment(-1))
            } else {
                transaction.set(likeRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
                transaction.update(postRef, "likes", FieldValue.increment(1))
            }
        }.await()
    }

    suspend fun toggleSave(userId: String, postId: String): Result<Unit> = runCatching {
        val currentlySaved = _globalSavedStatus.value[postId] 
            ?: savedPostsMemoryCache.isSaved(postId) 
            ?: (postDao.getPostById(postId)?.isSavedByMe ?: false)
        updateSavedStatusLocal(postId, !currentlySaved)

        val savedRef = firestore.collection("users").document(userId)
            .collection("saved_posts").document(postId)
        val doc = savedRef.get().await()
        if (doc.exists()) {
            savedRef.delete().await()
            savedPostsMemoryCache.setSaved(postId, false)
            updateSavedStatusLocal(postId, false)
            postDao.updateSavedStatus(postId, false)
        } else {
            savedRef.set(mapOf("savedAt" to FieldValue.serverTimestamp())).await()
            savedPostsMemoryCache.setSaved(postId, true)
            updateSavedStatusLocal(postId, true)
            postDao.updateSavedStatus(postId, true)
        }
        Unit
    }.onFailure { e ->
        val currentlySaved = _globalSavedStatus.value[postId] ?: false
        updateSavedStatusLocal(postId, !currentlySaved)
        savedPostsMemoryCache.setSaved(postId, !currentlySaved)
        android.util.Log.e(TAG, "Error toggling save for $postId: ${e.message}", e)
    }

    suspend fun toggleVote(userId: String, postId: String, voteValue: Int): Result<Unit> = runCatching {
        val currentVote = userVoteDao.getVoteOnce(postId, userId)?.voteValue ?: 0
        val nextVote = if (currentVote == voteValue) 0 else voteValue

        val localPost = postDao.getPostById(postId)
        val currentTruth = localPost?.truthCount ?: 0
        val currentFalse = localPost?.falseCount ?: 0

        val truthDelta = (if (nextVote == 1) 1 else 0) - (if (currentVote == 1) 1 else 0)
        val falseDelta  = (if (nextVote == -1) 1 else 0) - (if (currentVote == -1) 1 else 0)

        updateVoteStatusLocal(postId, VoteOverlay(
            truthCount = (currentTruth + truthDelta).coerceAtLeast(0),
            falseCount = (currentFalse + falseDelta).coerceAtLeast(0),
            myVote = nextVote
        ))

        try {
            val voteRef = database.getReference("user_votes").child(userId).child(postId)
            val postRef = firestore.collection("posts").document(postId)

            if (nextVote == 0) {
                voteRef.removeValue().await()
                postRef.update(
                    "truthCount", FieldValue.increment(if (voteValue == 1) -1L else 0L),
                    "falseCount", FieldValue.increment(if (voteValue == -1) -1L else 0L),
                    "votesScore", FieldValue.increment(-voteValue.toLong())
                ).await()
            } else {
                voteRef.setValue(nextVote).await()
                val truthInc = if (nextVote == 1) 1L else if (currentVote == 1) -1L else 0L
                val falseInc = if (nextVote == -1) 1L else if (currentVote == -1) -1L else 0L
                val scoreInc = if (currentVote == 0) nextVote.toLong() else (nextVote - currentVote).toLong()
                postRef.update(
                    "truthCount", FieldValue.increment(truthInc),
                    "falseCount", FieldValue.increment(falseInc),
                    "votesScore", FieldValue.increment(scoreInc)
                ).await()
            }

            if (nextVote == 0) {
                userVoteDao.deleteVote(postId, userId)
            } else {
                userVoteDao.upsertVote(UserVoteEntity(postId, userId, nextVote))
            }
        } catch (e: Exception) {
            updateVoteStatusLocal(postId, VoteOverlay(
                truthCount = currentTruth,
                falseCount = currentFalse,
                myVote = currentVote
            ))
            throw e
        }
    }
}
