package com.farbalapps.rinde.data.repository.delegate

import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.dao.UserVoteDao
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity
import com.farbalapps.rinde.data.local.entity.UserVoteEntity
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.data.util.SavedPostsMemoryCache
import com.farbalapps.rinde.data.worker.CreatePostWorker
import com.farbalapps.rinde.data.worker.UpdatePostWorker
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.repository.VoteOverlay
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class FeedLifecycleDelegate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val workManager: WorkManager,
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val userVoteDao: UserVoteDao,
    private val savedPostsMemoryCache: SavedPostsMemoryCache,
    private val firebaseAuth: FirebaseAuth
) {
    companion object {
        private const val TAG = "FeedLifecycleDelegate"
        private const val FEED_KEY = "feed_global"
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
    }

    suspend fun enrichPost(post: CommunityPost): CommunityPost {
        val userId = firebaseAuth.currentUser?.uid ?: ""
        val localPost = postDao.getPostById(post.id)
        val finalVote = if (userId.isNotEmpty()) {
            userVoteDao.getVoteOnce(post.id, userId)?.voteValue ?: 0
        } else 0
        val isSaved = savedPostsMemoryCache.isSaved(post.id) ?: localPost?.isSavedByMe ?: false
        return post.copy(myVoteValue = finalVote, isSavedByMe = isSaved)
    }

    suspend fun enrichPosts(posts: List<CommunityPost>): List<CommunityPost> {
        val userId = firebaseAuth.currentUser?.uid ?: ""
        val ids = posts.map { it.id }
        val localMap = postDao.getPostsByIds(ids).associateBy { it.id }
        val votesMap = if (userId.isNotEmpty() && ids.isNotEmpty()) {
            userVoteDao.getVotesForPosts(userId, ids).associateBy { it.postId }
        } else emptyMap()
        return posts.map { post ->
            val local = localMap[post.id]
            val vote = votesMap[post.id]?.voteValue ?: 0
            val saved = savedPostsMemoryCache.isSaved(post.id) ?: local?.isSavedByMe ?: false
            post.copy(myVoteValue = vote, isSavedByMe = saved)
        }
    }

    suspend fun snapshotToPosts(snapshot: com.google.firebase.firestore.QuerySnapshot?): List<CommunityPost> {
        val posts = snapshot?.documents?.mapNotNull { doc ->
            doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
        } ?: emptyList()
        return enrichPosts(posts)
    }

    fun getPostById(postId: String): Flow<CommunityPost> = callbackFlow {
        val listener = firestore.collection("posts").document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(TAG, "Error observing post $postId: ${error.message}")
                    return@addSnapshotListener
                }
                val postDto = snapshot?.toObject(CommunityPostDto::class.java)
                if (postDto != null) {
                    launch(Dispatchers.IO) {
                        try {
                            val post = postDto.copy(id = snapshot.id).toDomain()
                            val enriched = enrichPost(post)
                            trySend(enriched)
                            val existingLocal = postDao.getPostById(postId)
                            val safeEntity = enriched.toEntity().copy(
                                myVoteValue = enriched.myVoteValue
                                    .takeIf { it != 0 }
                                    ?: existingLocal?.myVoteValue
                                    ?: 0,
                                isSavedByMe = if (enriched.isSavedByMe) true
                                    else existingLocal?.isSavedByMe ?: false
                            )
                            postDao.insertPosts(listOf(safeEntity))
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Error enriching post $postId: ${e.message}", e)
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    fun getUserPosts(userId: String): Flow<List<CommunityPost>> {
        return postDao.getPostsByAuthorId(userId).map { entities ->
            val result = mutableListOf<CommunityPost>()
            for (entity in entities) {
                result.add(enrichPost(entity.toDomainModel()))
            }
            result
        }
    }

    suspend fun countNewPostsSince(sinceTimestamp: Long): Int {
        val localCount = postDao.countPostsNewerThan(sinceTimestamp)
        if (localCount > 0) return localCount

        return try {
            val query = firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .whereGreaterThan("timestamp", Date(sinceTimestamp))
            
            val aggregateQuery = query.count().get(AggregateSource.SERVER).await()
            aggregateQuery.count.toInt()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in countNewPostsSince: ${e.message}")
            0
        }
    }

    suspend fun refreshFeedIfNeeded(forceRefresh: Boolean): Result<Int> = runCatching {
        val meta = syncMetadataDao.getMetadata(FEED_KEY)
        val now = System.currentTimeMillis()
        if (!forceRefresh && meta != null && (now - meta.lastSyncTimestamp) < CACHE_TTL_MS) {
            return Result.success(0)
        }

        val sinceTimestamp = meta?.lastSyncTimestamp ?: 0L
        val snapshot = firestore.collection("posts")
            .whereEqualTo("isActive", true)
            .whereGreaterThan("timestamp", Date(sinceTimestamp))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()

        val posts = snapshot.documents.mapNotNull { doc ->
            doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
        }

        if (posts.isNotEmpty()) {
            postDao.upsertPosts(posts.map { it.toEntity() })
        }

        syncMetadataDao.upsert(
            (meta ?: SyncMetadataEntity(key = FEED_KEY)).copy(
                lastSyncTimestamp = now
            )
        )
        posts.size
    }

    suspend fun forceExpireCache() {
        syncMetadataDao.upsert(SyncMetadataEntity(key = FEED_KEY, lastSyncTimestamp = 0L))
    }

    suspend fun syncUserVotes(userId: String): Result<Unit> = runCatching {
        val snapshot = database.getReference("user_votes").child(userId).get().await()
        if (snapshot.exists()) {
            val voteUpdates = mutableMapOf<String, VoteOverlay>()
            val entitiesToUpsert = mutableListOf<UserVoteEntity>()
            snapshot.children.forEach { voteNode ->
                val postId = voteNode.key ?: return@forEach
                val voteValue = voteNode.getValue(Int::class.java) ?: return@forEach
                
                entitiesToUpsert.add(UserVoteEntity(postId, userId, voteValue))
                
                voteUpdates[postId] = VoteOverlay(
                    truthCount = null,
                    falseCount = null,
                    myVote = voteValue
                )
            }
            if (entitiesToUpsert.isNotEmpty()) {
                userVoteDao.upsertVotes(entitiesToUpsert)
            }
            // Retornar las actualizaciones para que el manager las propague si es necesario
            voteUpdates
        }
    }.map { Unit }

    suspend fun syncUserSavedPosts(userId: String): Result<Unit> = runCatching {
        val snapshot = firestore.collection("users").document(userId)
            .collection("saved_posts").get().await()
        snapshot.documents.forEach { doc ->
            savedPostsMemoryCache.setSaved(doc.id, true)
        }
    }

    suspend fun uploadPost(post: CommunityPost, photoUris: List<String>): Result<Unit> = runCatching {
        if (photoUris.isEmpty()) throw Exception("Debes incluir al menos una imagen")
        val localPaths = photoUris.mapNotNull { uriString ->
            com.farbalapps.rinde.util.ImageOptimizer.optimizeImage(context, Uri.parse(uriString))?.absolutePath
        }.toTypedArray()
        if (localPaths.isEmpty()) throw Exception("Error procesando las imágenes")

        val inputMap = mutableMapOf<String, Any>(
            "authorId" to post.authorId,
            "authorName" to post.authorName,
            "title" to post.title,
            "descriptionLong" to post.descriptionLong,
            "category" to post.category,
            "locationName" to post.location.name,
            "localFilePaths" to localPaths,
            "offerType" to post.offerType.name,
            "currency" to post.currency,
            "isAvailable" to post.isAvailable,
            "condition" to post.condition,
            "userReputationScore" to post.userReputationScore
        )
        post.authorPhotoUrl?.let { inputMap["authorPhotoUrl"] = it }
        post.websiteName?.let { inputMap["websiteName"] = it }
        post.productLink?.let { inputMap["productLink"] = it }
        post.storeName?.let { inputMap["storeName"] = it }
        post.normalPrice?.let { inputMap["normalPrice"] = it }
        post.discountPrice?.let { inputMap["discountPrice"] = it }
        post.couponCode?.let { inputMap["couponCode"] = it }
        post.discountPercentage?.let { inputMap["discountPercentage"] = it }

        val workData = androidx.work.Data.Builder().putAll(inputMap).build()

        workManager.enqueue(
            OneTimeWorkRequestBuilder<CreatePostWorker>()
                .setInputData(workData)
                .build()
        )
    }

    suspend fun deletePost(postId: String, photoUrls: List<String>): Result<Unit> = runCatching {
        postDao.updatePostStatus(postId, false)
        firestore.collection("posts").document(postId).delete().await()
        android.util.Log.d(TAG, "Post eliminado de Firestore: $postId. Las imágenes quedan huérfanas en Cloudinary.")
        postDao.deletePostById(postId)
    }

    suspend fun updatePost(
        postId: String, title: String, descriptionLong: String, category: String,
        normalPrice: Double?, discountPrice: Double?, currency: String,
        couponCode: String?, discountPercentage: Int?, isAvailable: Boolean,
        condition: String, websiteName: String?, productLink: String?,
        storeName: String?, locationName: String, oldPhotoUrls: List<String>,
        newPhotoUris: List<String>
    ): Result<Unit> = runCatching {
        val localPaths = newPhotoUris.mapNotNull { uriString ->
            com.farbalapps.rinde.util.ImageOptimizer.optimizeImage(context, android.net.Uri.parse(uriString))?.absolutePath
        }.toTypedArray()

        val inputMap = mutableMapOf<String, Any>(
            "postId" to postId,
            "title" to title,
            "descriptionLong" to descriptionLong,
            "category" to category,
            "locationName" to locationName,
            "localFilePaths" to localPaths,
            "oldPhotoUrls" to oldPhotoUrls.toTypedArray(),
            "currency" to currency,
            "isAvailable" to isAvailable,
            "condition" to condition
        )
        android.util.Log.d(TAG, "Post eliminado de Firestore: $postId. Las imágenes quedan huérfanas en Cloudinary por seguridad.")

        websiteName?.let { inputMap["websiteName"] = it }
        productLink?.let { inputMap["productLink"] = it }
        storeName?.let { inputMap["storeName"] = it }
        normalPrice?.let { inputMap["normalPrice"] = it }
        discountPrice?.let { inputMap["discountPrice"] = it }
        couponCode?.let { inputMap["couponCode"] = it }
        discountPercentage?.let { inputMap["discountPercentage"] = it }

        val workData = androidx.work.Data.Builder().putAll(inputMap).build()

        workManager.enqueue(
            OneTimeWorkRequestBuilder<UpdatePostWorker>()
                .setInputData(workData)
                .build()
        )
    }

    suspend fun markPostAsExpired(postId: String): Result<Unit> = runCatching {
        postDao.updateVerificationStatus(postId, "EXPIRED")
        try {
            firestore.collection("posts").document(postId)
                .update("verificationStatus", "EXPIRED").await()
        } catch (e: Exception) {
            postDao.updateVerificationStatus(postId, "PENDING")
            throw e
        }
    }

    suspend fun markPostAsAvailable(postId: String): Result<Unit> = runCatching {
        postDao.updateVerificationStatus(postId, "PENDING")
        try {
            firestore.collection("posts").document(postId)
                .update("verificationStatus", "PENDING").await()
        } catch (e: Exception) {
            postDao.updateVerificationStatus(postId, "EXPIRED")
            throw e
        }
    }

    suspend fun reportPostAsExpired(
        postId: String, postTitle: String, authorId: String,
        currentUserId: String, currentUserName: String
    ): Result<Unit> = runCatching {
        firestore.collection("expiration_reports").add(mapOf(
            "postId" to postId,
            "postTitle" to postTitle,
            "authorId" to authorId,
            "reportedBy" to currentUserId,
            "reportedByName" to currentUserName,
            "timestamp" to FieldValue.serverTimestamp()
        )).await()
    }
}
