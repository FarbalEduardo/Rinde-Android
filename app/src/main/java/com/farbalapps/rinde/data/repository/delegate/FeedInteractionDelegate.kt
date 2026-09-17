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

import com.farbalapps.rinde.data.local.dao.PendingVoteDao
import com.farbalapps.rinde.data.local.entity.PendingVoteEntity
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.mapper.toSavedSnapshotMap
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.data.remote.model.SavedPostSnapshotDto
import com.farbalapps.rinde.data.worker.VoteSyncWorker
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max

class FeedInteractionDelegate @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val postDao: PostDao,
    private val userVoteDao: UserVoteDao,
    private val pendingVoteDao: PendingVoteDao,
    private val savedPostsMemoryCache: SavedPostsMemoryCache,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
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
        android.util.Log.d(TAG, "🔖 Iniciando Saved Posts para user: $userId (Snapshot optimizado)")

        val savedListener = firestore.collection("users").document(userId)
            .collection("saved_posts")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .limit(FEED_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(TAG, "❌ Error al escuchar saved_posts: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    try {
                        val legacyDocIds = mutableListOf<String>()
                        val resultPosts = mutableListOf<CommunityPost>()

                        for (doc in snapshot.documents) {
                            val title = doc.getString("title")
                            if (!title.isNullOrBlank()) {
                                // 1. Desnormalizado directo: parsear snapshot sin queries secundarias
                                val snapshotDto = doc.toObject(SavedPostSnapshotDto::class.java)
                                if (snapshotDto != null && snapshotDto.isActive) {
                                    resultPosts.add(snapshotDto.copy(postId = doc.id).toDomain())
                                }
                            } else {
                                // 2. Documento legado (solo tenía savedAt): marcar para fallback
                                legacyDocIds.add(doc.id)
                            }
                        }

                        // Si hay documentos legados antiguos, resolverlos mediante fallback
                        if (legacyDocIds.isNotEmpty()) {
                            val legacyPosts = legacyDocIds.chunked(30).flatMap { chunk ->
                                firestore.collection("posts")
                                    .whereEqualTo("isActive", true)
                                    .whereIn(FieldPath.documentId(), chunk)
                                    .get().await()
                                    .documents.mapNotNull { d ->
                                        d.toObject(CommunityPostDto::class.java)?.copy(id = d.id)?.toDomain()
                                            ?.copy(isSavedByMe = true)
                                    }
                            }
                            resultPosts.addAll(legacyPosts)
                        }

                        // Preservar el orden cronológico de guardado según snapshot
                        val orderedPosts = snapshot.documents.mapNotNull { doc ->
                            resultPosts.find { it.id == doc.id }
                        }

                        trySend(orderedPosts)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "❌ Error procesando saved_posts: ${e.message}", e)
                        trySend(emptyList())
                    }
                }
            }

        awaitClose {
            savedListener.remove()
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
            // Generar el snapshot desnormalizado para acceso ultrarrápido O(1)
            val localPostEntity = postDao.getPostById(postId)
            val snapshotMap = if (localPostEntity != null) {
                localPostEntity.toDomainModel().toSavedSnapshotMap()
            } else {
                try {
                    val remoteDoc = firestore.collection("posts").document(postId).get().await()
                    val remotePost = remoteDoc.toObject(CommunityPostDto::class.java)?.copy(id = remoteDoc.id)?.toDomain()
                    remotePost?.toSavedSnapshotMap() ?: mapOf("postId" to postId, "savedAt" to FieldValue.serverTimestamp())
                } catch (e: Exception) {
                    mapOf("postId" to postId, "savedAt" to FieldValue.serverTimestamp())
                }
            }
            savedRef.set(snapshotMap).await()
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
        val overlay = _globalVoteStatus.value[postId]

        val hasSynced = overlay == null || (localPost?.myVoteValue == overlay.myVote)
        
        val currentTruth = if (hasSynced) localPost?.truthCount ?: 0 else overlay!!.truthCount ?: 0
        val currentFalse = if (hasSynced) localPost?.falseCount ?: 0 else overlay!!.falseCount ?: 0

        val truthDelta = (if (nextVote == 1) 1 else 0) - (if (currentVote == 1) 1 else 0)
        val falseDelta  = (if (nextVote == -1) 1 else 0) - (if (currentVote == -1) 1 else 0)

        updateVoteStatusLocal(postId, VoteOverlay(
            truthCount = (currentTruth + truthDelta).coerceAtLeast(0),
            falseCount = (currentFalse + falseDelta).coerceAtLeast(0),
            myVote = nextVote
        ))

        try {
            val voteRef = database.getReference("post_votes").child(postId).child(userId)
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

    suspend fun toggleVoteTransaction(userId: String, postId: String, voteValue: Int): Result<Triple<Int, Int, Int>> = runCatching {
        val voteDocRef = firestore.collection("posts")
            .document(postId).collection("votes").document(userId)
        val postRef = firestore.collection("posts").document(postId)

        var finalCounts = Triple(0, 0, 0)
        var finalNextVote = 0
        firestore.runTransaction { transaction ->
            val voteSnapshot = transaction.get(voteDocRef)
            val serverVote = voteSnapshot.getLong("value")?.toInt() ?: 0
            val nextVote = if (serverVote == voteValue) 0 else voteValue
            finalNextVote = nextVote

            val postSnapshot = transaction.get(postRef)
            val currentTruth = postSnapshot.getLong("truthCount")?.toInt() ?: 0
            val currentFalse = postSnapshot.getLong("falseCount")?.toInt() ?: 0
            val currentScore = postSnapshot.getLong("votesScore")?.toInt() ?: 0

            val truthDelta = (if (nextVote == 1) 1 else 0) - (if (serverVote == 1) 1 else 0)
            val falseDelta  = (if (nextVote == -1) 1 else 0) - (if (serverVote == -1) 1 else 0)
            val scoreDelta  = nextVote - serverVote

            if (nextVote == 0) {
                transaction.delete(voteDocRef)
            } else {
                transaction.set(voteDocRef, mapOf(
                    "value" to nextVote,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }

            val newTruth = (currentTruth + truthDelta).coerceAtLeast(0)
            val newFalse = (currentFalse + falseDelta).coerceAtLeast(0)
            val newScore = currentScore + scoreDelta

            if (truthDelta != 0 || falseDelta != 0 || scoreDelta != 0) {
                transaction.update(postRef,
                    "truthCount", newTruth.toLong(),
                    "falseCount", newFalse.toLong(),
                    "votesScore", newScore.toLong()
                )
            }
            finalCounts = Triple(newTruth, newFalse, newScore)
        }.await()

        // Sincronizar localmente en Room y estado global
        if (finalNextVote == 0) {
            userVoteDao.deleteVote(postId, userId)
        } else {
            userVoteDao.upsertVote(UserVoteEntity(postId, userId, finalNextVote))
        }

        updateVoteStatusLocal(
            postId,
            VoteOverlay(
                truthCount = finalCounts.first,
                falseCount = finalCounts.second,
                myVote = finalNextVote
            )
        )

        finalCounts
    }

    suspend fun fetchPostVoteCounts(postId: String): Result<Triple<Int, Int, Int>> = runCatching {
        val doc = firestore.collection("posts").document(postId).get().await()
        Triple(
            doc.getLong("truthCount")?.toInt() ?: 0,
            doc.getLong("falseCount")?.toInt() ?: 0,
            doc.getLong("votesScore")?.toInt() ?: 0
        )
    }

    suspend fun savePendingVote(userId: String, postId: String, voteValue: Int, authorId: String) {
        pendingVoteDao.upsert(
            PendingVoteEntity(
                postId = postId,
                userId = userId,
                voteValue = voteValue,
                authorId = authorId
            )
        )
    }

    private data class OriginalVoteState(
        val vote: Int,
        val truthCount: Int,
        val falseCount: Int,
        val score: Int
    )

    private val optimisticVoteBackups = ConcurrentHashMap<String, OriginalVoteState>()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun handleOfflineVote(userId: String, postId: String, voteValue: Int, authorId: String) {
        savePendingVote(userId, postId, voteValue, authorId)
        val request = OneTimeWorkRequestBuilder<VoteSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "vote_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun applyOptimisticVote(postId: String, voteValue: Int) {
        val existingPost = postDao.getPostById(postId) ?: return
        val origVote = existingPost.myVoteValue ?: 0
        val origTruth = existingPost.truthCount
        val origFalse = existingPost.falseCount
        val origScore = existingPost.votesScore

        optimisticVoteBackups[postId] = OriginalVoteState(origVote, origTruth, origFalse, origScore)

        val nextVote = if (origVote == voteValue) 0 else voteValue
        val tDelta = (if (nextVote == 1) 1 else 0) - (if (origVote == 1) 1 else 0)
        val fDelta = (if (nextVote == -1) 1 else 0) - (if (origVote == -1) 1 else 0)

        val optTruth = (origTruth + tDelta).coerceAtLeast(0)
        val optFalse = (origFalse + fDelta).coerceAtLeast(0)
        val optScore = origScore + (nextVote - origVote)

        postDao.updateVoteState(postId, nextVote, optTruth, optFalse, optScore)
        updateVoteStatusLocal(
            postId,
            VoteOverlay(
                truthCount = optTruth,
                falseCount = optFalse,
                myVote = nextVote
            )
        )
    }

    suspend fun revertOptimisticVote(postId: String) {
        val backup = optimisticVoteBackups.remove(postId)
        if (backup != null) {
            postDao.updateVoteState(postId, backup.vote, backup.truthCount, backup.falseCount, backup.score)
            updateVoteStatusLocal(
                postId,
                VoteOverlay(
                    truthCount = backup.truthCount,
                    falseCount = backup.falseCount,
                    myVote = backup.vote
                )
            )
        }
    }

    suspend fun syncLocalVoteAfterSuccess(postId: String, voteValue: Int, counts: Triple<Int, Int, Int>) {
        val backup = optimisticVoteBackups.remove(postId)
        val (truth, false_, score) = counts
        val local = postDao.getPostById(postId)
        val originalVote = backup?.vote ?: 0
        val currentVote = local?.myVoteValue ?: (if (originalVote == voteValue) 0 else voteValue)
        if (local != null) {
            postDao.updateVoteState(postId, currentVote, truth, false_, score)
        }
        updateVoteStatusLocal(
            postId,
            VoteOverlay(
                truthCount = truth,
                falseCount = false_,
                myVote = currentVote
            )
        )
    }

    suspend fun recalculateAuthorTrustScore(authorId: String): Result<Unit> = runCatching {
        val postsSnapshot = firestore.collection("posts")
            .whereEqualTo("authorId", authorId)
            .get().await()

        if (postsSnapshot.isEmpty) return@runCatching

        val now = Date().time
        val postsToUpdate = mutableListOf<String>()
        var totalWeightedScore = 0f
        var totalWeight = 0f
        var verifiedCount = 0

        for (doc in postsSnapshot.documents) {
            val post = doc.toObject(CommunityPostDto::class.java) ?: continue
            postsToUpdate.add(doc.id)

            val truthCount = post.truthCount
            val falseCount = post.falseCount
            val totalVotes = truthCount + falseCount

            if (totalVotes < 3) continue

            var postScore = (truthCount.toFloat() / totalVotes.toFloat()) * 5f
            val status = runCatching { VerificationStatus.valueOf(post.verificationStatus) }.getOrNull()
            if (status == VerificationStatus.EXPIRED || status == VerificationStatus.DISPUTED) {
                postScore -= 0.5f
            }

            if (truthCount.toFloat() / totalVotes.toFloat() >= 0.8f) {
                postScore += 0.3f
                verifiedCount++
            }

            postScore = postScore.coerceIn(0f, 5f)

            val daysSincePost = ((now - (post.timestamp?.time ?: now)) / (1000 * 60 * 60 * 24)).toFloat()
            val weight = 1f / (1f + max(0f, daysSincePost) * 0.05f)

            totalWeightedScore += (postScore * weight)
            totalWeight += weight
        }

        val finalTrustScore = if (totalWeight > 0f) totalWeightedScore / totalWeight else 0f
        val trustLevel = when {
            finalTrustScore >= 4.5f -> "PLATINUM"
            finalTrustScore >= 3.5f -> "GOLD"
            finalTrustScore >= 2.5f -> "SILVER"
            finalTrustScore >= 1.5f -> "BRONZE"
            else -> "NEW"
        }

        firestore.runBatch { batch ->
            val userRef = firestore.collection("users").document(authorId)
            batch.set(
                userRef,
                mapOf(
                    "trustScore" to finalTrustScore,
                    "trustLevel" to trustLevel,
                    "verifiedPostsCount" to verifiedCount
                ),
                SetOptions.merge()
            )

            for (postId in postsToUpdate) {
                val postRef = firestore.collection("posts").document(postId)
                batch.update(
                    postRef,
                    mapOf(
                        "authorTrustScore" to finalTrustScore,
                        "authorTrustLevel" to trustLevel
                    )
                )
            }
        }.await()
    }
}

