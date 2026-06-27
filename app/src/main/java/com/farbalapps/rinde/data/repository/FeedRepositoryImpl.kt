package com.farbalapps.rinde.data.repository

import android.content.Context
import android.net.Uri
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.local.entity.CommunityPostEntity
import com.farbalapps.rinde.data.local.entity.SyncMetadataEntity
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.remote.PostRemoteMediator
import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.data.util.SavedPostsMemoryCache
import com.farbalapps.rinde.data.util.VotesMemoryCache
import com.farbalapps.rinde.data.worker.CreatePostWorker
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.util.ImageOptimizer
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldPath
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

class FeedRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val workManager: WorkManager,
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val votesMemoryCache: VotesMemoryCache,
    private val savedPostsMemoryCache: SavedPostsMemoryCache
) : FeedRepository {

    companion object {
        private const val TAG = "FeedRepositoryImpl"
        private const val FEED_LIMIT = 30L
        private const val FEED_KEY = "feed_global"
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
    }

    private suspend fun enrichPost(post: CommunityPost): CommunityPost {
        val localPost = postDao.getPostById(post.id)
        val finalVote = votesMemoryCache.getVote(post.id) ?: localPost?.myVoteValue ?: 0
        val isSaved = savedPostsMemoryCache.isSaved(post.id) ?: localPost?.isSavedByMe ?: false
        return post.copy(myVoteValue = finalVote, isSavedByMe = isSaved)
    }

    private suspend fun enrichPosts(posts: List<CommunityPost>): List<CommunityPost> {
        val ids = posts.map { it.id }
        val localMap = postDao.getPostsByIds(ids).associateBy { it.id }
        return posts.map { post ->
            val local = localMap[post.id]
            val vote = votesMemoryCache.getVote(post.id) ?: local?.myVoteValue ?: 0
            val saved = savedPostsMemoryCache.isSaved(post.id) ?: local?.isSavedByMe ?: false
            post.copy(myVoteValue = vote, isSavedByMe = saved)
        }
    }

    private suspend fun snapshotToPosts(snapshot: com.google.firebase.firestore.QuerySnapshot?): List<CommunityPost> {
        val posts = snapshot?.documents?.mapNotNull { doc ->
            doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)?.toDomain()
        } ?: emptyList()
        return enrichPosts(posts)
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getPagedFeed(forceRefresh: Boolean): Flow<PagingData<CommunityPost>> {
        android.util.Log.d(TAG, "getPagedFeed call (forceRefresh=$forceRefresh)")
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 3
            ),
            remoteMediator = PostRemoteMediator(
                firestore = firestore,
                postDao = postDao,
                syncMetadataDao = syncMetadataDao,
                forceRefresh = forceRefresh
            ),
            pagingSourceFactory = { postDao.getPostsPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                val domain = entity.toDomainModel()
                enrichPost(domain)
            }
        }
    }

    override fun getUserPosts(userId: String): Flow<List<CommunityPost>> {
        android.util.Log.d(TAG, "Observing user posts from Room for: $userId")
        return postDao.getPostsByAuthorId(userId).map { entities ->
            val result = mutableListOf<CommunityPost>()
            for (entity in entities) {
                result.add(enrichPost(entity.toDomainModel()))
            }
            result
        }
    }

    override suspend fun countNewPostsSince(sinceTimestamp: Long): Int {
        val localCount = postDao.countPostsNewerThan(sinceTimestamp)
        if (localCount > 0) return localCount

        android.util.Log.d(TAG, "Counting new posts since timestamp $sinceTimestamp using count() aggregation query.")
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

    override suspend fun refreshFeedIfNeeded(forceRefresh: Boolean): Result<Int> = runCatching {
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

    override suspend fun forceExpireCache() {
        syncMetadataDao.upsert(SyncMetadataEntity(key = FEED_KEY, lastSyncTimestamp = 0L))
    }

    override fun getFollowingFeed(userId: String, lastPostId: String?): Flow<List<CommunityPost>> = callbackFlow {
        android.util.Log.d(TAG, "👥 Iniciando Following Feed para user: $userId")

        var postsListener: com.google.firebase.firestore.ListenerRegistration? = null

        val relationshipsListener = firestore.collection("relationships")
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { relSnapshot, relError ->
                if (relError != null) return@addSnapshotListener

                val followedIds = relSnapshot?.documents
                    ?.mapNotNull { it.getString("followedId") }
                    ?: emptyList()

                if (followedIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                launch {
                    val cached = postDao.getPostsByAuthorIdsOnce(followedIds, limit = 30)
                    if (cached.isNotEmpty()) {
                        trySend(enrichPosts(cached.map { it.toDomainModel() }))
                    }
                }

                postsListener?.remove()

                postsListener = firestore.collection("posts")
                    .whereEqualTo("isActive", true)
                    .whereIn("authorId", followedIds.take(30))
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(FEED_LIMIT)
                    .addSnapshotListener { postSnapshot, postError ->
                        if (postError != null) {
                            android.util.Log.e(TAG, "❌ Error en Following Feed posts: ${postError.message}")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        launch {
                            val posts = snapshotToPosts(postSnapshot)
                            trySend(posts)
                            if (posts.isNotEmpty()) {
                                postDao.insertPosts(posts.map { it.toEntity() })
                            }
                        }
                    }
            }

        awaitClose {
            relationshipsListener.remove()
            postsListener?.remove()
        }
    }

    override fun getSavedPosts(userId: String): Flow<List<CommunityPost>> = callbackFlow {
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
                            trySend(posts)
                        }
                    }
            }

        awaitClose {
            savedListener.remove()
            postsListener?.remove()
        }
    }

    override fun getNearbyFeed(lat: Double, lon: Double, radiusKm: Double): Flow<List<CommunityPost>> = callbackFlow {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(lat)))

        val listener = firestore.collection("posts")
            .whereEqualTo("isActive", true)
            .whereGreaterThanOrEqualTo("location.latitude", lat - latDelta)
            .whereLessThanOrEqualTo("location.latitude", lat + latDelta)
            .limit(FEED_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e(TAG, "❌ Error en Nearby Feed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                launch {
                    val posts = snapshotToPosts(snapshot).filter { post ->
                        post.location.longitude != null &&
                        post.location.longitude >= (lon - lonDelta) &&
                        post.location.longitude <= (lon + lonDelta)
                    }.sortedByDescending { it.timestamp }
                    trySend(posts)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getSmartInterestFeed(interests: List<String>, lastPostId: String?): Flow<List<CommunityPost>> = callbackFlow {
        val query = if (interests.isNotEmpty()) {
            firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .whereIn("category", interests.take(10))
                .orderBy("votesScore", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        } else {
            firestore.collection("posts")
                .whereEqualTo("isActive", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        }.limit(FEED_LIMIT)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e(TAG, "❌ Error en Smart Interest Feed: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }
            launch {
                val posts = snapshotToPosts(snapshot)
                trySend(posts)
                if (posts.isNotEmpty()) {
                    postDao.insertPosts(posts.map { it.toEntity() })
                }
            }
        }

        awaitClose { listener.remove() }
    }

    override fun getPostById(postId: String): Flow<CommunityPost> = callbackFlow {
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
                            postDao.insertPosts(listOf(enriched.toEntity()))
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Error enriching post $postId: ${e.message}", e)
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun syncUserVotes(userId: String): Result<Unit> = runCatching {
        val snapshot = database.getReference("user_votes").child(userId).get().await()
        if (snapshot.exists()) {
            snapshot.children.forEach { voteNode ->
                val postId = voteNode.key ?: return@forEach
                val voteValue = voteNode.getValue(Int::class.java) ?: return@forEach
                votesMemoryCache.setVote(postId, voteValue)
            }
        }
    }

    override suspend fun syncUserSavedPosts(userId: String): Result<Unit> = runCatching {
        val snapshot = firestore.collection("users").document(userId)
            .collection("saved_posts").get().await()
        snapshot.documents.forEach { doc ->
            savedPostsMemoryCache.setSaved(doc.id, true)
        }
    }

    override suspend fun uploadPost(post: CommunityPost, photoUris: List<String>): Result<Unit> = runCatching {
        if (photoUris.isEmpty()) throw Exception("Debes incluir al menos una imagen")
        val localPaths = photoUris.mapNotNull { uriString ->
            ImageOptimizer.optimizeImage(context, Uri.parse(uriString))?.absolutePath
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

    override suspend fun toggleLike(userId: String, postId: String): Result<Unit> = runCatching {
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

    override suspend fun toggleSave(userId: String, postId: String): Result<Unit> = runCatching {
        val savedRef = firestore.collection("users").document(userId)
            .collection("saved_posts").document(postId)
        val doc = savedRef.get().await()
        if (doc.exists()) {
            savedRef.delete().await()
            savedPostsMemoryCache.setSaved(postId, false)
        } else {
            savedRef.set(mapOf("savedAt" to FieldValue.serverTimestamp())).await()
            savedPostsMemoryCache.setSaved(postId, true)
        }
    }

    override suspend fun toggleVote(userId: String, postId: String, voteValue: Int): Result<Unit> = runCatching {
        val voteRef = database.getReference("user_votes").child(userId).child(postId)
        val postRef = firestore.collection("posts").document(postId)
        val currentVote = voteRef.get().await().getValue(Int::class.java) ?: 0

        if (currentVote == voteValue) {
            voteRef.removeValue().await()
            postRef.update("votesScore", FieldValue.increment(-voteValue.toLong())).await()
            votesMemoryCache.setVote(postId, 0)
        } else {
            voteRef.setValue(voteValue).await()
            val increment = if (currentVote == 0) voteValue.toLong() else (voteValue - currentVote).toLong()
            postRef.update("votesScore", FieldValue.increment(increment)).await()
            votesMemoryCache.setVote(postId, voteValue)
        }
    }

    override suspend fun deletePost(postId: String, photoUrls: List<String>): Result<Unit> = runCatching {
        // Marcamos como inactivo localmente primero para feedback instantáneo
        postDao.updatePostStatus(postId, false)
        
        // Eliminamos físicamente de Firestore
        firestore.collection("posts").document(postId).delete().await()
            
        // Eliminar las imágenes de Cloudinary en segundo plano (fire-and-forget)
        photoUrls.forEach { url ->
            if (url.contains("cloudinary.com")) {
                try {
                    com.farbalapps.rinde.util.CloudinaryHelper.deleteImage(url)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error al eliminar imagen de Cloudinary: $url", e)
                }
            }
        }

        // Eliminar físicamente de Room para limpiar el caché
        postDao.deletePostById(postId)
    }

    override suspend fun updatePost(
        postId: String, title: String, descriptionLong: String, category: String,
        normalPrice: Double?, discountPrice: Double?, currency: String,
        couponCode: String?, discountPercentage: Int?, isAvailable: Boolean,
        condition: String, websiteName: String?, productLink: String?,
        storeName: String?, locationName: String, oldPhotoUrls: List<String>,
        newPhotoUris: List<String>
    ): Result<Unit> = runCatching {
        val updates = mapOf(
            "title" to title,
            "descriptionLong" to descriptionLong,
            "category" to category,
            "normalPrice" to normalPrice,
            "discountPrice" to discountPrice,
            "currency" to currency,
            "couponCode" to couponCode,
            "discountPercentage" to discountPercentage,
            "isAvailable" to isAvailable,
            "condition" to condition,
            "websiteName" to websiteName,
            "productLink" to productLink,
            "storeName" to storeName,
            "location.name" to locationName
        )
        // Update firestore first
        firestore.collection("posts").document(postId).update(updates).await()

        // Fetch latest document and update Room cache
        val doc = firestore.collection("posts").document(postId).get().await()
        val updatedPostDto = doc.toObject(CommunityPostDto::class.java)?.copy(id = doc.id)
        if (updatedPostDto != null) {
            val domain = updatedPostDto.toDomain()
            val enriched = enrichPost(domain)
            postDao.upsertPosts(listOf(enriched.toEntity()))
        }
    }

    override suspend fun markPostAsExpired(postId: String): Result<Unit> = runCatching {
        firestore.collection("posts").document(postId)
            .update("verificationStatus", "EXPIRED").await()
        postDao.updateVerificationStatus(postId, "EXPIRED")
    }

    override suspend fun reportPostAsExpired(
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

    override fun getUnreadNotificationsCount(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("notifications")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }
}
