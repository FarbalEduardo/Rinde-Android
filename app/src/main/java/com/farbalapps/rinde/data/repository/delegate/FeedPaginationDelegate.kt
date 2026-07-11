package com.farbalapps.rinde.data.repository.delegate

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.data.local.dao.SyncMetadataDao
import com.farbalapps.rinde.data.remote.HotPostRemoteMediator
import com.farbalapps.rinde.data.remote.PostRemoteMediator
import com.farbalapps.rinde.domain.model.CommunityPost
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import com.farbalapps.rinde.data.local.entity.toDomainModel
import com.farbalapps.rinde.data.local.entity.toEntity
import com.farbalapps.rinde.data.mapper.toDomain
import javax.inject.Inject

class FeedPaginationDelegate @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val postDao: PostDao,
    private val syncMetadataDao: SyncMetadataDao
) {
    companion object {
        private const val TAG = "FeedPaginationDelegate"
        private const val FEED_LIMIT = 30L
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getPagedFeed(
        forceRefresh: Boolean,
        enrichPost: suspend (CommunityPost) -> CommunityPost
    ): Flow<PagingData<CommunityPost>> {
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
                enrichPost(entity.toDomainModel())
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getHotPagedFeed(
        forceRefresh: Boolean,
        enrichPost: suspend (CommunityPost) -> CommunityPost
    ): Flow<PagingData<CommunityPost>> {
        android.util.Log.d(TAG, "getHotPagedFeed call (forceRefresh=$forceRefresh)")
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 3
            ),
            remoteMediator = HotPostRemoteMediator(
                firestore = firestore,
                postDao = postDao,
                syncMetadataDao = syncMetadataDao,
                forceRefresh = forceRefresh
            ),
            pagingSourceFactory = { postDao.getHotPostsPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { entity ->
                enrichPost(entity.toDomainModel())
            }
        }
    }

    fun getNearbyFeed(
        lat: Double,
        lon: Double,
        radiusKm: Double,
        snapshotToPosts: suspend (com.google.firebase.firestore.QuerySnapshot?) -> List<CommunityPost>
    ): Flow<List<CommunityPost>> = callbackFlow {
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

    fun getSmartInterestFeed(
        interests: List<String>,
        snapshotToPosts: suspend (com.google.firebase.firestore.QuerySnapshot?) -> List<CommunityPost>
    ): Flow<List<CommunityPost>> = callbackFlow {
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
}
